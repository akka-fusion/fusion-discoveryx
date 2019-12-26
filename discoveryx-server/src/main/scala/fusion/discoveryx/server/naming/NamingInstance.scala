/*
 * Copyright 2019 akka-fusion.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fusion.discoveryx.server.naming

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import akka.stream.scaladsl.Tcp
import fusion.discoveryx.common.{ Constants, Protocols }
import fusion.discoveryx.model.{ HealthyCheckMethod, Instance, InstanceModify }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

// NamingService 需要判断 NamingInstance actor 是否存在
object NamingInstance {
  trait Command

  final case class Get(groupName: String, isHealthy: Boolean, replyTo: ActorRef[Option[Instance]]) extends Command
  final case class Modify(in: InstanceModify, replyTo: ActorRef[Instance]) extends Command
  final case object Remove extends Command

  private case class SniffResult(isSuccess: Boolean, time: Long = System.currentTimeMillis()) extends Command
  private case object Heartbeat extends Command
  private case object HealthyCheckTick extends Command

  private[naming] def apply(
      instance: Instance,
      serviceRef: ActorRef[NamingService.Command],
      settings: NamingSettings): Behavior[Command] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new NamingInstance(formatInstance(instance), serviceRef, settings, timers, context).init()))

  private def formatInstance(inst: Instance): Instance =
    inst.copy(
      serviceName = if (inst.serviceName.isEmpty) Constants.DEFAULT_GROUP_NAME else inst.serviceName,
      healthyCheckMethod =
        if (inst.healthyCheckMethod == HealthyCheckMethod.NOT_SET) HealthyCheckMethod.CLIENT_REPORT
        else inst.healthyCheckMethod,
      unhealthyCheckCount = if (inst.unhealthyCheckCount < 1) 1 else inst.unhealthyCheckCount,
      protocol = Protocols.formatProtocol(inst.protocol))

  private def instanceModify(old: Instance, in: InstanceModify): Instance = {
    old.copy(
      groupName = in.groupName.getOrElse(old.groupName),
      ip = in.ip.getOrElse(old.ip),
      port = in.port.getOrElse(old.port),
      weight = in.weight.getOrElse(old.weight),
      healthy = in.health.getOrElse(old.healthy),
      enabled = in.enable.getOrElse(old.enabled),
      metadata = if (in.replaceMetadata) in.metadata else old.metadata ++ in.metadata,
      healthyCheckMethod =
        if (in.healthyCheckMethod == HealthyCheckMethod.NOT_SET || in.healthyCheckMethod.isUnrecognized)
          old.healthyCheckMethod
        else in.healthyCheckMethod,
      healthyCheckInterval = in.healthyCheckInterval.getOrElse(old.healthyCheckInterval),
      unhealthyCheckCount = in.unhealthyCheckCount.getOrElse(old.unhealthyCheckCount),
      protocol = in.protocol.map(Protocols.formatProtocol).getOrElse(old.protocol))
  }
}

import fusion.discoveryx.server.naming.NamingInstance._
class NamingInstance private (
    private var instance: Instance,
    serviceRef: ActorRef[NamingService.Command],
    settings: NamingSettings,
    timer: TimerScheduler[Command],
    context: ActorContext[Command]) {
  private val UNREACHABLE_TIMEOUT =
    if (instance.healthyCheckInterval < 1) settings.heartbeatTimeout.toMillis else instance.healthyCheckInterval * 1000L
  private var lastActivityTime = System.currentTimeMillis()
  private var unreachableCount = 0

  def init(): Behavior[Command] = {
    timer.startTimerWithFixedDelay(HealthyCheckTick, healthyCheckInterval)
    instance.healthyCheckMethod match {
      case HealthyCheckMethod.TCP_SNIFF | HealthyCheckMethod.UDP_SNIFF => activeSniff()
      case _                                                           => clientReport()
    }
  }

  private def healthyCheckInterval: FiniteDuration =
    if (instance.healthyCheckInterval > 0L) instance.healthyCheckInterval.seconds else settings.heartbeatTimeout

  private def tryChangeHealthy(): Unit = {
    if (unreachableCount < instance.unhealthyCheckCount) {
      if (!instance.healthy) {
        instance = instance.copy(healthy = true)
      }
    } else {
      instance = instance.copy(healthy = false)
      unreachableCount = 0
    }
  }

  private def receive: Command => Behavior[Command] = {
    case in: Get             => processGet(in)
    case Modify(in, replyTo) => processModify(in, replyTo)
    case Remove              => Behaviors.stopped
    case _                   => Behaviors.unhandled
  }

  private def processGet(in: Get): Behavior[Command] = {
    val beGroupName = in.groupName.isEmpty || instance.groupName.contains(in.groupName)
    val beHealthy = !in.isHealthy || instance.healthy
    val resp = if (beGroupName && beHealthy) Some(instance) else None
    in.replyTo ! resp
    Behaviors.same
  }

  private def processModify(in: InstanceModify, replyTo: ActorRef[Instance]): Behavior[Command] = {
    val old = instance
    instance = instanceModify(instance, in)
    replyTo ! instance
    if (old.healthyCheckMethod != instance.healthyCheckMethod) {
      instance.healthyCheckMethod match {
        case HealthyCheckMethod.CLIENT_REPORT                            => clientReport()
        case HealthyCheckMethod.TCP_SNIFF | HealthyCheckMethod.UDP_SNIFF => activeSniff()
        case HealthyCheckMethod.NOT_SET                                  => Behaviors.same
      }
    } else
      Behaviors.same
  }

  ////////////////////////////////////////////////////////////////
  // clientReport
  ////////////////////////////////////////////////////////////////
  def clientReport(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case Heartbeat =>
      lastActivityTime = System.currentTimeMillis()
      unreachableCount = 0
      Behaviors.same

    case HealthyCheckTick =>
      if ((System.currentTimeMillis() - lastActivityTime) >= UNREACHABLE_TIMEOUT) {
        unreachableCount += 1
      }
      tryChangeHealthy()
      Behaviors.same

    case message =>
      receive(message)
  }

  ////////////////////////////////////////////////////////////////
  // activeSniff
  ////////////////////////////////////////////////////////////////
  def activeSniff(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case HealthyCheckTick =>
      sendSniffRequest()

    case SniffResult(isSuccess, time) =>
      if (isSuccess) {
        lastActivityTime = time
      } else {
        unreachableCount += 1
      }
      tryChangeHealthy()
      Behaviors.same

    case message =>
      receive(message)
  }

  private def sendSniffRequest(): Behavior[Command] = {
    // TODO send TCP or UDP request
    //      更改 unreachableCount 值
    //      unreachableCount 与 instance.

    val sniffResultF = instance.protocol match {
      case Protocols.TCP   => sniffTcp(instance.ip, instance.port)
      case Protocols.UDP   => sniffUdp(instance.ip, instance.port)
      case Protocols.HTTP  => sniffHttp(Protocols.HTTP, instance.ip, instance.port)
      case Protocols.HTTPS => sniffHttp(Protocols.HTTPS, instance.ip, instance.port)
    }

    context.pipeToSelf(sniffResultF) {
      case Success(value) =>
        if (!value) {
          context.log.debug(s"Sniff '${instance.protocol}://${instance.ip}:${instance.port}' failure, return false.")
        }
        SniffResult(value)
      case Failure(e) =>
        context.log.debug(
          s"Sniff '${instance.protocol}://${instance.ip}:${instance.port}' failure, exception: ${e.getLocalizedMessage}.")
        SniffResult(false)
    }

    Behaviors.same
  }

  private def sniffTcp(ip: String, port: Int): Future[Boolean] = {
    Tcp(context.system).bind(instance.ip, instance.port)
    ???
  }

  private def sniffUdp(ip: String, port: Int): Future[Boolean] = {
    ???
  }

  private def sniffHttp(protocol: String, ip: String, port: Int): Future[Boolean] = {
    import context.executionContext
    Http(context.system)
      .singleRequest(HttpRequest(HttpMethods.GET, s"$protocol://$ip:$port/"))
      .map(_.status.isSuccess())
  }
}

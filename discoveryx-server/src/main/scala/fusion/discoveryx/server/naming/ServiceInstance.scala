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
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, PostStop, Terminated }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import fusion.discoveryx.DiscoveryXUtils
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.HSBadRequestException
import helloscala.common.util.StringUtils

import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

object ServiceInstance {
  trait Command
  trait Event

  private case object HealthCheckKey extends Command
  private[naming] case class InternalHealthyChanged(inst: Instance, healthy: Boolean) extends Command

  object ServiceKey {
    def unapply(entityId: String): Option[NamingServiceKey] = entityId.split(' ') match {
      case Array(namespace, serviceName) =>
        Some(new NamingServiceKey(namespace, serviceName, Constants.DEFAULT_GROUP_NAME))
      case _ => None
    }
  }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ServiceInstance")

  def entityId(namespace: String, serviceName: String): Either[String, String] = {
    if (StringUtils.isBlank(namespace) || StringUtils.isBlank(serviceName))
      Left("entityId invalid, need '[namespace] [serviceName]' format.")
    else Right(s"$namespace $serviceName")
  }

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(ServiceInstance.TypeKey)(entityContext => apply(entityContext.entityId))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))

  private def apply(entityId: String): Behavior[Command] = Behaviors.setup[Command] { context =>
    context.log.debug(s"apply entityId is '$entityId'")
    val namingServiceKey = ServiceKey
      .unapply(entityId)
      .getOrElse(throw HSBadRequestException(
        s"${context.self} create child error. entityId invalid, need '[namespace] [serviceName]' format."))
    Behaviors.withTimers(timers => new ServiceInstance(entityId, namingServiceKey, timers, context).receive())
  }
}

class ServiceInstance private (
    entityId: String,
    private var serviceKey: NamingServiceKey,
    timers: TimerScheduler[ServiceInstance.Command],
    context: ActorContext[ServiceInstance.Command]) {
  import ServiceInstance._
  private val settings = NamingSettings(context.system)
  private val internalService = new InternalService(serviceKey, settings, context.self)
  private var listeners: Map[ActorRef[Event], ServiceListener] = Map()

  NamingManager.init(context.system) ! ShardingEnvelope(serviceKey.namespace, NamingRegisterToManager(entityId))
  timers.startTimerWithFixedDelay(HealthCheckKey, HealthCheckKey, settings.heartbeatTimeout)
  context.log.info(s"ServiceInstance started: $serviceKey")

  def receive(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case NamingReplyCommand(replyTo, cmd) =>
          replyTo ! processReplyCommand(cmd)
          Behaviors.same
        case Heartbeat(_, _, instId) =>
          processHeartbeat(instId)
        case QueryServiceInfo(replyTo) =>
          replyTo ! queryServiceInfo()
          Behaviors.same
        case HealthCheckKey =>
          healthCheck()
        case NamingListenerCommand(ref, in) =>
          listeners = listeners.updated(ref, in)
          context.watch(ref)
          Behaviors.same
        case InternalHealthyChanged(inst, healthy) =>
          notifyServiceEventListeners(inst, healthy)
        case _: StopServiceInstance =>
          Behaviors.stopped
      }
      .receiveSignal {
        case (_, PostStop) =>
          cleanup()
          Behaviors.same
        case (_, Terminated(ref)) =>
          try {
            listeners = listeners.removed(ref.unsafeUpcast[Event])
          } catch {
            case NonFatal(_) => // do nothing
          }
          Behaviors.same
      }

  private def cleanup(): Unit = {
    for ((ref, _) <- listeners) {
      ref ! ServiceEventStop()
    }
  }

  private def queryServiceInfo(): ServiceInfo = {
    ServiceInfo(
      serviceKey.namespace,
      serviceKey.serviceName,
      serviceKey.groupName,
      serviceKey.protectThreshold,
      serviceKey.metadata,
      internalService.allRealInstance())
  }

  private def healthCheck(): Behavior[Command] = {
    internalService.checkHealthy()
    Behaviors.same
  }

  private def processHeartbeat(instanceId: String): Behavior[Command] = {
    internalService.processHeartbeat(instanceId)
    Behaviors.same
  }

  private def processReplyCommand(cmd: NamingReplyCommand.Cmd): NamingReply = {
    import NamingReplyCommand.Cmd
    val reply = cmd match {
      case Cmd.Query(value)    => queryInstance(value)
      case Cmd.Register(value) => registerInstance(value)
      case Cmd.Remove(value)   => removeInstance(value)
      case Cmd.Modify(value)   => modifyInstance(value)
      case Cmd.CreateService(value) =>
        serviceKey = serviceKey.copy(
          groupName = if (StringUtils.isBlank(value.groupName)) Constants.DEFAULT_GROUP_NAME else value.groupName,
          protectThreshold = value.protectThreshold,
          metadata = value.metadata)
        NamingReply(IntStatus.OK, data = NamingReply.Data.ServiceInfo(queryServiceInfo()))
      case Cmd.ModifyService(value) =>
        val old = serviceKey
        serviceKey = serviceKey.copy(
          groupName = value.groupName.filterNot(str => StringUtils.isBlank(str)).getOrElse(old.groupName),
          protectThreshold = value.protectThreshold.getOrElse(old.protectThreshold),
          metadata = if (value.replaceMetadata) value.metadata else old.metadata ++ value.metadata)
        NamingReply(IntStatus.OK, data = NamingReply.Data.ServiceInfo(queryServiceInfo()))
      case Cmd.RemoveService(_) =>
        context.self ! StopServiceInstance()
        NamingReply(IntStatus.OK)
      case Cmd.Empty => NamingReply(IntStatus.BAD_REQUEST, "Invalid Cmd.")
    }

    if (IntStatus.isSuccess(reply.status)) {
      notifyServiceEventListeners(cmd, reply.data)
    }

    reply
  }

  private def notifyServiceEventListeners(cmd: NamingReplyCommand.Cmd, data: NamingReply.Data): Unit = {
    val changeType = cmd match {
      case _: NamingReplyCommand.Cmd.Register      => NamingChangeType.INSTANCE_REGISTER
      case _: NamingReplyCommand.Cmd.Modify        => NamingChangeType.INSTANCE_MODIFY
      case _: NamingReplyCommand.Cmd.Remove        => NamingChangeType.INSTANCE_REMOVE
      case _: NamingReplyCommand.Cmd.CreateService => NamingChangeType.SERVICE_CREATE
      case _: NamingReplyCommand.Cmd.ModifyService => NamingChangeType.SERVICE_MODIFY
      case _: NamingReplyCommand.Cmd.RemoveService => NamingChangeType.SERVICE_REMOVE
      case _                                       => return
    }
    val event = NamingServiceEvent(
      ServiceEvent(
        changeType,
        serviceKey.namespace,
        serviceKey.serviceName,
        data.serviceInfo.map(si =>
          NamingServiceKey(si.namespace, si.serviceName, si.groupName, si.protectThreshold, si.metadata)),
        data.instance))
    for ((ref, _) <- listeners) {
      ref ! event
    }
  }

  private def notifyServiceEventListeners(inst: Instance, healthy: Boolean): Behavior[Command] = {
    val event = NamingServiceEvent(
      ServiceEvent(
        if (healthy) NamingChangeType.INSTANCE_HEALTHY else NamingChangeType.INSTANCE_UNHEALTHY,
        serviceKey.namespace,
        serviceKey.serviceName,
        instance = Some(inst)))
    for ((ref, _) <- listeners) {
      ref ! event
    }
    Behaviors.same
  }

  private def queryInstance(in: InstanceQuery): NamingReply =
    try {
      val items = internalService.queryInstance(in)
      NamingReply(
        IntStatus.OK,
        data = NamingReply.Data.ServiceInfo(
          ServiceInfo(
            serviceKey.namespace,
            serviceKey.serviceName,
            serviceKey.groupName,
            serviceKey.protectThreshold,
            serviceKey.metadata,
            items)))
    } catch {
      case _: IllegalArgumentException => NamingReply(IntStatus.BAD_REQUEST)
    }

  private def modifyInstance(in: InstanceModify): NamingReply =
    try {
      internalService.modifyInstance(in) match {
        case Some(inst) => NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
        case None       => NamingReply(IntStatus.NOT_FOUND)
      }
    } catch {
      case _: IllegalArgumentException => NamingReply(IntStatus.BAD_REQUEST)
    }

  private def removeInstance(in: InstanceRemove): NamingReply = {
    NamingReply(if (internalService.removeInstance(in.instanceId)) IntStatus.OK else IntStatus.NOT_FOUND)
  }

  private def registerInstance(in: InstanceRegister): NamingReply =
    try {
      val inst = internalService.addInstance(DiscoveryXUtils.toInstance(in))
      NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
    } catch {
      case _: IllegalArgumentException => NamingReply(IntStatus.BAD_REQUEST)
    }
}

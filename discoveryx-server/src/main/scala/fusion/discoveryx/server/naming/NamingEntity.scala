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
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.pubsub.{ DistributedPubSub, DistributedPubSubMediator }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import fusion.discoveryx.DiscoveryXUtils
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.HSBadRequestException
import helloscala.common.util.StringUtils

object NamingEntity {
  trait Command

  private case object HealthCheckKey extends Command
  private case object StopNaming extends Command

  object NamingServiceKey {
    def unapply(entityId: String): Option[NamingServiceKey] = entityId.split(' ') match {
      case Array(namespace, serviceName) => Some(new NamingServiceKey(namespace, serviceName))
      case _                             => None
    }
  }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("NamingEntity")

  def entityId(namespace: String, serviceName: String): Either[String, String] = {
    if (StringUtils.isBlank(namespace) || StringUtils.isBlank(serviceName))
      Left("entityId invalid, need '[namespace] [serviceName]' format.")
    else Right(s"$namespace $serviceName")
  }

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(NamingEntity.TypeKey)(entityContext => apply(entityContext.entityId)))

  private def apply(entityId: String): Behavior[Command] = Behaviors.setup[Command] { context =>
    context.log.debug(s"apply entityId is '$entityId'")
    val namingServiceKey = NamingServiceKey
      .unapply(entityId)
      .getOrElse(throw HSBadRequestException(
        s"${context.self} create child error. entityId invalid, need '[namespace] [serviceName]' format."))
    Behaviors.withTimers(timers => new NamingEntity(namingServiceKey, timers, context).receive())
  }
}

class NamingEntity private (
    namingServiceKey: NamingServiceKey,
    timers: TimerScheduler[NamingEntity.Command],
    context: ActorContext[NamingEntity.Command]) {
  import NamingEntity._
  private val settings = NamingSettings(context.system)
  private val internalService = new InternalService(namingServiceKey, settings)

  DistributedPubSub(context.system).mediator ! DistributedPubSubMediator.Publish(
    NamingManager.TOPIC_NAMING_TO_MANAGER,
    NamingRegisterToManager(context.self))
  timers.startTimerWithFixedDelay(HealthCheckKey, HealthCheckKey, settings.heartbeatInterval)
  context.log.info(s"Namings started: $namingServiceKey")

  def receive(): Behavior[Command] = Behaviors.receiveMessage {
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
    case StopNaming =>
      Behaviors.stopped
  }

  private def queryServiceInfo(): ServiceInfo = {
    ServiceInfo(
      namingServiceKey.namespace,
      namingServiceKey.serviceName,
      "", // TODO group_name
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
    cmd match {
      case Cmd.Query(value)         => queryInstance(value)
      case Cmd.Register(value)      => registerInstance(value)
      case Cmd.Remove(value)        => removeInstance(value)
      case Cmd.Modify(value)        => modifyInstance(value)
      case Cmd.CreateService(value) =>
        // TODO 保存 value.groupName
        NamingReply(IntStatus.OK, data = NamingReply.Data.ServiceInfo(queryServiceInfo()))
      case Cmd.ModifyService(value) =>
        // TODO 保存 value.groupName
        NamingReply(IntStatus.OK, data = NamingReply.Data.ServiceInfo(queryServiceInfo()))
      case Cmd.RemoveService(_) =>
        context.self ! StopNaming
        NamingReply(IntStatus.OK)
      case Cmd.Empty => NamingReply(IntStatus.BAD_REQUEST, "Invalid Cmd.")
    }
  }

  private def queryInstance(in: InstanceQuery): NamingReply =
    try {
      val items = internalService.queryInstance(in)
      val status = if (items.isEmpty) IntStatus.NOT_FOUND else IntStatus.OK
      NamingReply(status, data = NamingReply.Data.Queried(InstanceQueryResult(items)))
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

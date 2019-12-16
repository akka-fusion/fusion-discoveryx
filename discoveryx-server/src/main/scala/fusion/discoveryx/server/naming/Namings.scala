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
import akka.cluster.pubsub.{ DistributedPubSub, DistributedPubSubMediator }
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import fusion.discoveryx.DiscoveryXUtils
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.HSBadRequestException
import helloscala.common.util.StringUtils

object Namings {
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Naming")

  trait Command
  trait ReplyCommand extends Command {
    @transient val replyTo: ActorRef[NamingReply]

    def withReplyTo(other: ActorRef[NamingReply]): ReplyCommand
  }

  private case object HealthCheckKey extends Command

  object NamingServiceKey {
    def entityId(namespace: String, serviceName: String): Either[String, String] = {
      if (StringUtils.isBlank(namespace) || StringUtils.isBlank(serviceName)) {
        Left("entityId invalid, need '[namespace] [serviceName]' format.")
      } else {
        Right(s"$namespace $serviceName")
      }
    }

    def unapply(entityId: String): Option[NamingServiceKey] = entityId.split(' ') match {
      case Array(namespace, serviceName) => Some(new NamingServiceKey(namespace, serviceName))
      case _                             => None
    }
  }

  def apply(entityId: String): Behavior[Command] = Behaviors.setup[Command] { context =>
    context.log.debug(s"apply entityId is '$entityId'")
    val namingServiceKey = NamingServiceKey
      .unapply(entityId)
      .getOrElse(throw HSBadRequestException(
        s"${context.self} create child error. entityId invalid, need '[namespace] [serviceName]' format."))
    Behaviors.withTimers(timers => new Namings(namingServiceKey, timers, context).receive())
  }
}

class Namings private (
    namingServiceKey: NamingServiceKey,
    timers: TimerScheduler[Namings.Command],
    context: ActorContext[Namings.Command]) {
  import Namings._
  private val settings = NamingSettings(context.system)
  private val internalService = new InternalService(namingServiceKey, settings)

  DistributedPubSub(context.system).mediator ! DistributedPubSubMediator.Publish(
    NamingManager.TOPIC_NAMING_TO_MANAGER,
    NamingRegisterToManager(context.self))
  timers.startTimerWithFixedDelay(HealthCheckKey, HealthCheckKey, settings.heartbeatInterval)
  context.log.info(s"Namings started: $namingServiceKey")

  def receive(): Behavior[Command] = Behaviors.receiveMessage {
    case Heartbeat(_, _, instanceId)   => processHeartbeat(instanceId)
    case QueryInstance(in, replyTo)    => queryInstance(in, replyTo)
    case RegisterInstance(in, replyTo) => registerInstance(in, replyTo)
    case RemoveInstance(in, replyTo)   => removeInstance(in, replyTo)
    case ModifyInstance(in, replyTo)   => modifyInstance(in, replyTo)
    case QueryServiceInfo(replyTo)     => queryServiceInfo(replyTo)
    case HealthCheckKey                => healthCheck()
  }

  private def queryServiceInfo(replyTo: ActorRef[ServiceInfo]): Behavior[Command] = {
    replyTo ! ServiceInfo(namingServiceKey.namespace, namingServiceKey.serviceName, internalService.allRealInstance())
    Behaviors.same
  }

  private def healthCheck(): Behavior[Command] = {
    internalService.checkHealthy()
    Behaviors.same
  }

  private def processHeartbeat(instanceId: String): Behavior[Command] = {
    internalService.processHeartbeat(instanceId)
    Behaviors.same
  }

  private def queryInstance(in: InstanceQuery, replyTo: ActorRef[NamingReply]): Behavior[Command] = {
    val result = try {
      val items = internalService.queryInstance(in)
      val status = if (items.isEmpty) IntStatus.NOT_FOUND else IntStatus.OK
      NamingReply(status, data = NamingReply.Data.Queried(InstanceQueryResult(items)))
    } catch {
      case _: IllegalArgumentException => NamingReply(IntStatus.BAD_REQUEST)
    }
    replyTo ! result
    Behaviors.same
  }

  private def modifyInstance(in: InstanceModify, replyTo: ActorRef[NamingReply]): Behavior[Command] = {
    val result = try {
      internalService.modifyInstance(in) match {
        case Some(inst) => NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
        case None       => NamingReply(IntStatus.NOT_FOUND)
      }
    } catch {
      case _: IllegalArgumentException => NamingReply(IntStatus.BAD_REQUEST)
    }
    replyTo ! result
    Behaviors.same
  }

  private def removeInstance(in: InstanceRemove, replyTo: ActorRef[NamingReply]): Behavior[Command] = {
    val status = if (internalService.removeInstance(in.instanceId)) IntStatus.OK else IntStatus.NOT_FOUND
    replyTo ! NamingReply(status)
    Behaviors.same
  }

  private def registerInstance(in: InstanceRegister, replyTo: ActorRef[NamingReply]): Behavior[Command] = {
    val result = try {
      val inst = internalService.addInstance(DiscoveryXUtils.toInstance(in))
      NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
    } catch {
      case _: IllegalArgumentException => NamingReply(IntStatus.BAD_REQUEST)
    }
    replyTo ! result
    Behaviors.same
  }
}

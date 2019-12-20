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

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Terminated }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.util.Timeout
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object ServiceManager {
  trait Command extends ServiceInstance.Command

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ServiceManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
    ClusterSharding(system).init(Entity(TypeKey)(entityContext => ServiceManager(entityContext.entityId)))
  }

  private def apply(namespace: String): Behavior[Command] =
    Behaviors.setup(context => new ServiceManager(namespace, context).receive())
}

import fusion.discoveryx.server.naming.ServiceManager._
class ServiceManager private (namespace: String, context: ActorContext[Command]) {
  import context.executionContext
  private implicit val timeout: Timeout = 10.seconds
  private implicit val system: ActorSystem[_] = context.system
  private val namingSettings = NamingSettings(context.system)
  private val namingRegion = ServiceInstance.init(context.system)
  private var namings = Vector.empty[ActorRef[ServiceInstance.Command]]

  def receive(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case NamingManagerCommand(replyTo, cmd) => onManagerCommand(cmd, replyTo)
        case NamingRegisterToManager(namingRef) =>
          context.log.debug(s"NamingRegisterToManager($namingRef)")
          namings = namings.filterNot(old => old.path.name == namingRef.path.name) :+ namingRef
          context.watch(namingRef)
          Behaviors.same
      }
      .receiveSignal {
        case (_, Terminated(ref)) =>
          namings = namings.filterNot(_ == ref)
          Behaviors.same
      }

  private def onManagerCommand(
      command: NamingManagerCommand.Cmd,
      replyTo: ActorRef[NamingResponse]): Behavior[Command] =
    command match {
      case NamingManagerCommand.Cmd.ListService(cmd)   => processListService(cmd, replyTo)
      case NamingManagerCommand.Cmd.GetService(cmd)    => processGetService(cmd, replyTo)
      case NamingManagerCommand.Cmd.CreateService(cmd) => processCreateService(cmd, replyTo)
      case NamingManagerCommand.Cmd.ModifyService(cmd) => processModifyService(cmd, replyTo)
      case NamingManagerCommand.Cmd.RemoveService(cmd) => processRemoveService(cmd, replyTo)
      case NamingManagerCommand.Cmd.Empty =>
        context.log.warn(s"Invalid message: ${NamingManagerCommand.Cmd.Empty}")
        Behaviors.same
    }

  private def processModifyService(cmd: ModifyService, replyTo: ActorRef[NamingResponse]): Behavior[Command] =
    askNaming(cmd.namespace, cmd.serviceName, NamingReplyCommand.Cmd.ModifyService(cmd), replyTo) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processRemoveService(cmd: RemoveService, replyTo: ActorRef[NamingResponse]): Behavior[Command] =
    askNaming(cmd.namespace, cmd.serviceName, NamingReplyCommand.Cmd.RemoveService(cmd), replyTo)(_ =>
      NamingResponse.Data.Empty)

  private def processGetService(cmd: GetService, replyTo: ActorRef[NamingResponse]): Behavior[Command] =
    askNaming(
      cmd.namespace,
      cmd.serviceName,
      NamingReplyCommand.Cmd.Query(InstanceQuery(cmd.namespace, cmd.serviceName)),
      replyTo) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processCreateService(cmd: CreateService, replyTo: ActorRef[NamingResponse]): Behavior[Command] =
    askNaming(cmd.namespace, cmd.serviceName, NamingReplyCommand.Cmd.CreateService(cmd), replyTo) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processListService(cmd: ListService, replyTo: ActorRef[NamingResponse]): Behavior[Command] = {
    val (page, size, offset) = namingSettings.findPageSizeOffset(cmd.page, cmd.size)
    if (offset < namings.size) {
      val ns = namings.slice(offset, offset + size)
      println(s"namings: $ns - $namings")
      println(s"[$offset,$size] $cmd $replyTo")
      val futures = ns.map { naming =>
        naming.ask[ServiceInfo](ref => QueryServiceInfo(ref))
      }
      Future.sequence(futures).onComplete {
        case Success(serviceInfos) =>
          replyTo ! NamingResponse(
            IntStatus.OK,
            data = NamingResponse.Data.ListedService(ListedService(serviceInfos, page, size, namings.size)))
        case Failure(exception) =>
          context.log.warn(s"ListService error, $exception")
          replyTo ! NamingResponse(IntStatus.INTERNAL_ERROR, exception.getMessage)
      }
    } else {
      replyTo ! NamingResponse(
        IntStatus.OK,
        s"offset: $offset, but namings size is ${namings.size}",
        NamingResponse.Data.ListedService(ListedService(Nil, page, size, namings.size)))
    }

    Behaviors.same
  }

  private def askNaming(
      namespace: String,
      serviceName: String,
      cmd: NamingReplyCommand.Cmd,
      replyTo: ActorRef[NamingResponse])(onSuccess: NamingReply => NamingResponse.Data): Behavior[Command] = {
    ServiceInstance.entityId(namespace, serviceName) match {
      case Right(entityId) =>
        namingRegion.ask[NamingReply](ref => ShardingEnvelope(entityId, NamingReplyCommand(ref, cmd))).onComplete {
          case Success(value) =>
            context.log.info(s"Send to naming return: $value")
            replyTo ! NamingResponse(
              value.status,
              value.message,
              data = if (IntStatus.isSuccess(value.status)) onSuccess(value) else NamingResponse.Data.Empty)
          case Failure(e) => replyTo ! NamingResponse(IntStatus.INTERNAL_ERROR, e.getMessage)
        }
      case Left(errMsg) => replyTo ! NamingResponse(IntStatus.BAD_REQUEST, errMsg)
    }

    Behaviors.same
  }
}

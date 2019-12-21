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
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.util.Timeout
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object NamingManager {
  trait Command extends ServiceInstance.Command

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("NamingManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
    ClusterSharding(system).init(Entity(TypeKey)(entityContext => NamingManager(entityContext.entityId)))
  }

  private def apply(namespace: String): Behavior[Command] =
    Behaviors.setup(context => new NamingManager(namespace, context).receive())
}

import fusion.discoveryx.server.naming.NamingManager._
class NamingManager private (namespace: String, context: ActorContext[Command]) {
  import context.executionContext
  private implicit val timeout: Timeout = 5.seconds
  private implicit val system: ActorSystem[_] = context.system
  private val namingSettings = NamingSettings(context.system)
  private val serviceInstanceRegion = ServiceInstance.init(context.system)
  private var serviceInstanceIds = Vector.empty[String]

  def receive(): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case NamingManagerCommand(replyTo, cmd) => onManagerCommand(cmd, replyTo)
      case NamingRegisterToManager(entityId) =>
        if (!serviceInstanceIds.contains(entityId)) {
          serviceInstanceIds = serviceInstanceIds :+ entityId
          context.log.debug(s"Received NamingRegisterToManager($entityId), add after size: ${serviceInstanceIds.size}")
        } else {
          context.log.debug(s"Received NamingRegisterToManager($entityId)")
        }
        Behaviors.same
    }

  private def onManagerCommand(
      command: NamingManagerCommand.Cmd,
      replyTo: ActorRef[NamingResponse]): Behavior[Command] = {
    command match {
      case NamingManagerCommand.Cmd.ListService(cmd)   => futureReply(processListService(cmd), replyTo)
      case NamingManagerCommand.Cmd.GetService(cmd)    => futureReply(processGetService(cmd), replyTo)
      case NamingManagerCommand.Cmd.CreateService(cmd) => futureReply(processCreateService(cmd), replyTo)
      case NamingManagerCommand.Cmd.ModifyService(cmd) => futureReply(processModifyService(cmd), replyTo)
      case NamingManagerCommand.Cmd.RemoveService(cmd) => futureReply(processRemoveService(cmd), replyTo)
      case NamingManagerCommand.Cmd.Empty =>
        context.log.warn(s"Invalid message: ${NamingManagerCommand.Cmd.Empty}")
    }
    Behaviors.same
  }

  @inline private def futureReply(f: Future[NamingResponse], replyTo: ActorRef[NamingResponse]): Unit = f.onComplete {
    case Success(reply) => replyTo ! reply
    case Failure(e)     => NamingResponse(IntStatus.INTERNAL_ERROR, e.getMessage)
  }

  private def processModifyService(cmd: ModifyService): Future[NamingResponse] =
    askNaming(cmd.namespace, cmd.serviceName, NamingReplyCommand.Cmd.ModifyService(cmd)) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processRemoveService(cmd: RemoveService): Future[NamingResponse] =
    askNaming(cmd.namespace, cmd.serviceName, NamingReplyCommand.Cmd.RemoveService(cmd))(_ => NamingResponse.Data.Empty)

  private def processGetService(cmd: GetService): Future[NamingResponse] =
    askNaming(
      cmd.namespace,
      cmd.serviceName,
      NamingReplyCommand.Cmd.Query(InstanceQuery(cmd.namespace, cmd.serviceName))) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processCreateService(cmd: CreateService): Future[NamingResponse] =
    askNaming(cmd.namespace, cmd.serviceName, NamingReplyCommand.Cmd.CreateService(cmd)) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processListService(cmd: ListService): Future[NamingResponse] = {
    val (page, size, offset) = namingSettings.findPageSizeOffset(cmd.page, cmd.size)
    if (offset < serviceInstanceIds.size) {
      val ns = serviceInstanceIds.slice(offset, offset + size)
      val futures = ns.map { entityId =>
        serviceInstanceRegion.ask[ServiceInfo](ref => ShardingEnvelope(entityId, QueryServiceInfo(ref)))
      }
      Future.sequence(futures).map { serviceInfos =>
        NamingResponse(
          IntStatus.OK,
          data = NamingResponse.Data.ListedService(ListedService(serviceInfos, page, size, serviceInstanceIds.size)))
      }
    } else {
      Future.successful(
        NamingResponse(
          IntStatus.OK,
          s"offset: $offset, but ServiceInstance size is ${serviceInstanceIds.size}",
          NamingResponse.Data.ListedService(ListedService(Nil, page, size, serviceInstanceIds.size))))
    }
  }

  private def askNaming(namespace: String, serviceName: String, cmd: NamingReplyCommand.Cmd)(
      onSuccess: NamingReply => NamingResponse.Data): Future[NamingResponse] =
    ServiceInstance.entityId(namespace, serviceName) match {
      case Right(entityId) =>
        serviceInstanceRegion.ask[NamingReply](ref => ShardingEnvelope(entityId, NamingReplyCommand(ref, cmd))).map {
          value =>
            context.log.info(s"Send to ServiceInstance return: $value")
            NamingResponse(
              value.status,
              value.message,
              data = if (IntStatus.isSuccess(value.status)) onSuccess(value) else NamingResponse.Data.Empty)
        }
      case Left(errMsg) =>
        Future.successful(NamingResponse(IntStatus.BAD_REQUEST, errMsg))
    }
}

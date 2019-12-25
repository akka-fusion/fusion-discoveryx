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
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, PostStop }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol.NamingManagerCommand.Cmd
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.util.StringUtils

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object NamingManager {
  trait Command extends ServiceInstance.Command

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("NamingManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
    ClusterSharding(system).init(
      Entity(TypeKey)(entityContext => NamingManager(entityContext.entityId))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))
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
  private var serviceNames = Vector.empty[String]

  def receive(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case NamingManagerCommand(replyTo, cmd) => onManagerCommand(cmd, replyTo)
        case ServiceCreated(serviceName) =>
          if (!serviceNames.contains(serviceName)) {
            serviceNames = serviceNames :+ serviceName
            context.log.debug(s"Received ServiceCreated($serviceName), add after size: ${serviceNames.size}")
          } else {
            context.log.debug(s"Received exists ServiceCreated($serviceName)")
          }
          Behaviors.same
        case ServiceRemoved(serviceName) =>
          serviceNames = serviceNames.filterNot(_ == serviceName)
          Behaviors.same
        case _: StopNamingManager =>
          Behaviors.stopped
      }
      .receiveSignal {
        case (_, PostStop) =>
          cleanup()
          Behaviors.same
      }

  private def cleanup(): Unit = {
    for {
      serviceName <- serviceNames
      entityId <- ServiceInstance.entityId(namespace, serviceName)
    } {
      serviceInstanceRegion ! ShardingEnvelope(entityId, StopServiceInstance())
    }
  }

  private def onManagerCommand(command: Cmd, replyTo: ActorRef[NamingResponse]): Behavior[Command] = {
    command match {
      case Cmd.ListService(in)   => futureReply(processListService(in), replyTo)
      case Cmd.GetService(in)    => futureReply(processGetService(in), replyTo)
      case Cmd.CreateService(in) => futureReply(processCreateService(in), replyTo)
      case Cmd.ModifyService(in) => futureReply(processModifyService(in), replyTo)
      case Cmd.RemoveService(in) => futureReply(processRemoveService(in), replyTo)
      case Cmd.Empty             => context.log.warn(s"Invalid message: ${Cmd.Empty}")
    }
    Behaviors.same
  }

  @inline private def futureReply(f: Future[NamingResponse], replyTo: ActorRef[NamingResponse]): Unit = f.onComplete {
    case Success(reply) => replyTo ! reply
    case Failure(e)     => NamingResponse(IntStatus.INTERNAL_ERROR, e.getMessage)
  }

  private def processModifyService(in: ModifyService): Future[NamingResponse] =
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.ModifyService(in)) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processRemoveService(in: RemoveService): Future[NamingResponse] =
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.RemoveService(in))(_ => NamingResponse.Data.Empty)

  private def processGetService(in: GetService): Future[NamingResponse] =
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Query(InstanceQuery(in.namespace, in.serviceName))) {
      value =>
        NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processCreateService(in: CreateService): Future[NamingResponse] =
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.CreateService(in)) { value =>
      NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
    }

  private def processListService(in: ListService): Future[NamingResponse] = {
    val (page, size, offset) = namingSettings.generatePageSizeOffset(in.page, in.size)
    if (offset < serviceNames.size) {
      Source(serviceNames)
        .filter { serviceName =>
          if (StringUtils.isNoneBlank(in.serviceName)) serviceName.contains(in.serviceName)
          else true
        }
        .mapConcat(serviceName => ServiceInstance.entityId(namespace, serviceName).toSeq)
        .mapAsync(math.max(8, size)) { entityId =>
          val cmd = NamingReplyCommand.Cmd.Query(
            InstanceQuery(serviceName = in.serviceName, groupName = in.groupName, allHealthy = in.allHealthy))
          serviceInstanceRegion.ask[NamingReply](ref => ShardingEnvelope(entityId, NamingReplyCommand(ref, cmd)))
        }
        .collect { case NamingReply(IntStatus.OK, _, NamingReply.Data.ServiceInfo(value)) => value }
        .drop(offset)
        .take(size)
        .runWith(Sink.seq)
        .map { serviceInfos =>
          NamingResponse(
            IntStatus.OK,
            data = NamingResponse.Data.ListedService(ListedService(serviceInfos, page, size, serviceNames.size)))
        }
    } else {
      Future.successful(
        NamingResponse(
          IntStatus.OK,
          s"offset: $offset, but ServiceInstance size is ${serviceNames.size}",
          NamingResponse.Data.ListedService(ListedService(Nil, page, size, serviceNames.size))))
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

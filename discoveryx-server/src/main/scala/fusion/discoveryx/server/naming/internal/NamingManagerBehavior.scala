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

package fusion.discoveryx.server.naming.internal

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem, PostStop }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.model.{ InstanceQuery, NamingReply }
import fusion.discoveryx.server.namespace.NamespaceManager
import fusion.discoveryx.server.naming.NamingManager._
import fusion.discoveryx.server.naming.{ NamingService, NamingSettings }
import fusion.discoveryx.server.protocol.NamingManagerCommand.Cmd
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.util.StringUtils

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

private[naming] class NamingManagerBehavior(namespace: String, context: ActorContext[Command]) {
  import context.executionContext
  private val namingSettings = NamingSettings(context.system)
  private val managementRef = NamespaceManager.init(context.system)
  private val namingService = NamingService.init(context.system)

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, NamingManagerState] =
    EventSourcedBehavior(
      PersistenceId.of(TypeKey.name, namespace),
      NamingManagerState.defaultInstance,
      commandHandler,
      eventHandler)
      .receiveSignal {
        case (state, PostStop) =>
          stopAllService(state)
      }
      .withRetention(namingSettings.retentionCriteria)

  private def commandHandler(state: NamingManagerState, command: Command): Effect[Event, NamingManagerState] =
    command match {
      case NamingManagerCommand(replyTo, cmd) => onManagerCommand(state, cmd, replyTo)
      case in: ServiceCreatedEvent            => Effect.persist(in)
      case in: ServiceRemovedEvent            => Effect.persist(in)
      case in: RemoveNamingManager            => Effect.persist(in).thenStop()
      case DummyNamingManager()               => Effect.none
    }

  private def eventHandler(state: NamingManagerState, event: Event): NamingManagerState = event match {
    case ServiceCreatedEvent(serviceName) =>
      val serviceNames =
        if (!state.serviceNames.contains(serviceName)) state.serviceNames :+ serviceName
        else state.serviceNames
      managementRef ! ServiceSizeChangedEvent(namespace, serviceNames.size)
      state.copy(serviceNames = serviceNames)

    case ServiceRemovedEvent(serviceName) =>
      val serviceNames = state.serviceNames.filterNot(_ == serviceName)
      managementRef ! ServiceSizeChangedEvent(namespace, serviceNames.size)
      state.copy(serviceNames = serviceNames)

    case RemoveNamingManager() =>
      removeAllService(state)
      managementRef ! ServiceSizeChangedEvent(namespace)
      NamingManagerState.defaultInstance
  }

  private def stopAllService(state: NamingManagerState): Unit = {
    for {
      serviceName <- state.serviceNames
      entityId <- NamingService.makeEntityId(namespace, serviceName)
    } {
      namingService ! ShardingEnvelope(entityId, StopServiceInstance())
    }
  }

  private def removeAllService(state: NamingManagerState): Unit = {
    for {
      serviceName <- state.serviceNames
      entityId <- NamingService.makeEntityId(namespace, serviceName)
    } {
      namingService ! ShardingEnvelope(entityId, RemoveServiceInstance())
    }
  }

  private def onManagerCommand(
      state: NamingManagerState,
      command: Cmd,
      replyTo: ActorRef[NamingResponse]): Effect[Event, NamingManagerState] = {
    command match {
      case Cmd.ListService(in)   => futureReply(processListService(state, in), replyTo)
      case Cmd.GetService(in)    => futureReply(processGetService(state, in), replyTo)
      case Cmd.CreateService(in) => futureReply(processCreateService(state, in), replyTo)
      case Cmd.ModifyService(in) => futureReply(processModifyService(state, in), replyTo)
      case Cmd.RemoveService(in) => futureReply(processRemoveService(state, in), replyTo)
      case Cmd.Empty             => context.log.warn(s"Invalid message: ${Cmd.Empty}")
    }
    Effect.none
  }

  @inline private def futureReply(f: Future[NamingResponse], replyTo: ActorRef[NamingResponse]): Unit = f.onComplete {
    case Success(reply) => replyTo ! reply
    case Failure(e)     => NamingResponse(IntStatus.INTERNAL_ERROR, e.getLocalizedMessage)
  }

  private def processModifyService(state: NamingManagerState, in: ModifyService): Future[NamingResponse] =
    if (state.serviceNames.contains(in.serviceName)) {
      askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.ModifyService(in)) { value =>
        NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
      }
    } else {
      Future.successful(NamingResponse(IntStatus.NOT_FOUND, s"Service name is [${in.serviceName}], it's not found."))
    }

  private def processRemoveService(state: NamingManagerState, in: RemoveService): Future[NamingResponse] =
    if (state.serviceNames.contains(in.serviceName)) {
      askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.RemoveService(in))(_ => NamingResponse.Data.Empty)
    } else {
      Future.successful(NamingResponse(IntStatus.NOT_FOUND, s"Service name is [${in.serviceName}], it's not found."))
    }

  private def processGetService(state: NamingManagerState, in: GetService): Future[NamingResponse] =
    if (state.serviceNames.contains(in.serviceName)) {
      askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Query(InstanceQuery(in.namespace, in.serviceName))) {
        value =>
          NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
      }
    } else {
      Future.successful(NamingResponse(IntStatus.NOT_FOUND, s"Service name is [${in.serviceName}], it's not found."))
    }

  private def processCreateService(state: NamingManagerState, in: CreateService): Future[NamingResponse] =
    if (state.serviceNames.contains(in.serviceName)) {
      Future.successful(
        NamingResponse(
          IntStatus.CONFLICT,
          s"Service name is conflict, the value [${in.serviceName}] is already exists."))
    } else {
      askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.CreateService(in)) { value =>
        NamingResponse.Data.ServiceInfo(value.data.serviceInfo.get)
      }
    }

  private implicit val timeout: Timeout = 5.seconds
  private implicit val system: ActorSystem[_] = context.system

  private def processListService(state: NamingManagerState, in: ListService): Future[NamingResponse] = {
    val serviceNames = state.serviceNames
    val (page, size, offset) = namingSettings.generatePageSizeOffset(in.page, in.size)
    if (offset < serviceNames.size) {
      Source(serviceNames)
        .filter { serviceName =>
          if (StringUtils.isNoneBlank(in.serviceName)) serviceName.contains(in.serviceName)
          else true
        }
        .mapConcat(serviceName => NamingService.makeEntityId(namespace, serviceName).toSeq)
        .mapAsync(math.max(8, size)) { entityId =>
          val cmd = NamingReplyCommand.Cmd.Query(
            InstanceQuery(serviceName = in.serviceName, groupName = in.groupName, allHealthy = in.allHealthy))
          namingService.ask[NamingReply](ref => ShardingEnvelope(entityId, NamingReplyCommand(ref, cmd)))
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
          s"The calculated offset [$offset] is greater than or equal to the length of the NamingService [${serviceNames.size}].",
          NamingResponse.Data.ListedService(ListedService(Nil, page, size, serviceNames.size))))
    }
  }

  private def askNaming(namespace: String, serviceName: String, cmd: NamingReplyCommand.Cmd)(
      onSuccess: NamingReply => NamingResponse.Data): Future[NamingResponse] =
    NamingService.makeEntityId(namespace, serviceName) match {
      case Right(entityId) =>
        namingService.ask[NamingReply](ref => ShardingEnvelope(entityId, NamingReplyCommand(ref, cmd))).map { value =>
          context.log.debug(s"NamingService return value is [$value].")
          NamingResponse(
            value.status,
            value.message,
            data = if (IntStatus.isSuccess(value.status)) onSuccess(value) else NamingResponse.Data.Empty)
        }
      case Left(errMsg) =>
        Future.successful(NamingResponse(IntStatus.BAD_REQUEST, errMsg))
    }
}

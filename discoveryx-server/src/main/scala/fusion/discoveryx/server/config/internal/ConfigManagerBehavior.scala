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

package fusion.discoveryx.server.config.internal

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, PostStop }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.model._
import fusion.discoveryx.server.config.ConfigManager._
import fusion.discoveryx.server.config.{ ConfigEntity, ConfigSettings }
import fusion.discoveryx.server.namespace.NamespaceManager
import fusion.discoveryx.server.protocol.ConfigManagerCommand.Cmd
import fusion.discoveryx.server.protocol.ConfigResponse.Data
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

private[config] class ConfigManagerBehavior(namespace: String, context: ActorContext[Command]) {
  import context.executionContext
  private val settings = ConfigSettings(context.system)
  private val configEntity = ConfigEntity.init(context.system)
  private val managementRef = NamespaceManager.init(context.system)

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, ConfigManagerState] =
    EventSourcedBehavior(
      PersistenceId.of(TypeKey.name, namespace),
      ConfigManagerState.defaultInstance,
      commandHandler,
      eventHandler).receiveSignal {
      case (state, PostStop) =>
        cleanup(state)
    }

  private def eventHandler(state: ConfigManagerState, event: Event): ConfigManagerState = event match {
    case ConfigAddedEvent(newDataId) =>
      val dataIds = if (state.dataIds.contains(newDataId)) state.dataIds else state.dataIds :+ newDataId
      managementRef ! ConfigSizeChangedEvent(namespace, dataIds.size)
      state.copy(dataIds = dataIds)

    case ConfigRemovedEvent(dataId) =>
      val dataIds = state.dataIds.filterNot(_ == dataId)
      managementRef ! ConfigSizeChangedEvent(namespace, dataIds.size)
      state.copy(dataIds = dataIds)

    case RemoveConfigManager() =>
      managementRef ! ConfigSizeChangedEvent(namespace)
      ConfigManagerState.defaultInstance
  }

  private def cleanup(state: ConfigManagerState): Unit = {
    for (dataId <- state.dataIds) {
      configEntity ! ShardingEnvelope(ConfigEntity.makeEntityId(namespace, dataId), RemoveAndStopConfigEntity())
    }
  }

  implicit val timeout: Timeout = 5.seconds

  private def commandHandler(state: ConfigManagerState, command: Command): Effect[Event, ConfigManagerState] =
    command match {
      case ConfigManagerCommand(replyTo, cmd)               => processManagerCommand(state, replyTo, cmd)
      case InternalConfigManagerResponse(replyTo, response) => processReplyToResponse(state, replyTo, response)
      case in: ConfigAddedEvent                             => Effect.persist(in)
      case in: ConfigRemovedEvent                           => Effect.persist(in)
      case DummyConfigManager()                             => Effect.none
      case StopConfigManager()                              => Effect.stop()
      case in: RemoveConfigManager                          => Effect.persist(in).thenStop()
    }

  private def processManagerCommand(
      state: ConfigManagerState,
      replyTo: ActorRef[ConfigResponse],
      command: Cmd): Effect[Event, ConfigManagerState] = {
    val future = command match {
      case Cmd.List(cmd) => processList(state, cmd)
      case Cmd.Get(cmd) =>
        if (state.dataIds.contains(cmd.dataId))
          askConfig(cmd.dataId, ConfigEntityCommand.Cmd.Get(cmd), _.config.map(Data.Config).getOrElse(Data.Empty))
        else
          Future.successful(ConfigResponse(IntStatus.NOT_FOUND))
      case Cmd.Publish(cmd) =>
        askConfig(
          cmd.dataId,
          ConfigEntityCommand.Cmd.Publish(cmd),
          _.config
            .map { item =>
              context.self ! ConfigAddedEvent(item.dataId)
              Data.Config(item)
            }
            .getOrElse(Data.Empty))
      case Cmd.Remove(cmd) =>
        if (state.dataIds.contains(cmd.dataId))
          askConfig(cmd.dataId, ConfigEntityCommand.Cmd.Remove(cmd), _ => Data.Empty)
        else
          Future.successful(ConfigResponse(IntStatus.NOT_FOUND))
      case Cmd.Empty => Future.successful(ConfigResponse(IntStatus.BAD_REQUEST, "Invalid command."))
    }

    context.pipeToSelf(future) {
      case Success(value) => InternalConfigManagerResponse(replyTo, value)
      case Failure(e) =>
        InternalConfigManagerResponse(replyTo, ConfigResponse(IntStatus.INTERNAL_ERROR, e.getLocalizedMessage))
    }
    Effect.none
  }

  private def processReplyToResponse(
      state: ConfigManagerState,
      replyTo: ActorRef[ConfigResponse],
      response: ConfigResponse): Effect[Event, ConfigManagerState] = {
    if (IntStatus.isSuccess(response.status)) {
      // TODO 发送事件
    }
    Effect.reply(replyTo)(response)
  }

  private def processList(state: ConfigManagerState, cmd: ListConfig): Future[ConfigResponse] = {
    implicit val system = context.system
    val (page, size, offset) = settings.generatePageSizeOffset(cmd.page, cmd.size)
    if (offset < state.dataIds.size) {
      Source(state.dataIds)
        .filter(dataId => cmd.dataId.forall(_ == dataId))
        .mapAsync(math.min(8, size)) { dataId =>
          askConfig(dataId, ConfigEntityCommand.Cmd.Query(ConfigQuery(cmd.groupName, cmd.tags)))
        }
        .collect { case Some(item) => itemToBasic(item) }
        .drop(offset)
        .take(size)
        .runWith(Sink.seq)
        .map { configs =>
          ConfigResponse(
            IntStatus.OK,
            data = Data.Listed(ConfigQueried(configs, namespace, page, size, state.dataIds.size)))
        }
    } else {
      Future.successful(
        ConfigResponse(
          IntStatus.OK,
          s"offset: $offset, but ConfigEntity size is ${state.dataIds.size}",
          Data.Listed(ConfigQueried(Nil, namespace, page, size, state.dataIds.size))))
    }
  }

  private def itemToBasic(item: ConfigItem) = ConfigBasic(item.dataId, item.groupName, item.`type`)

  private def askConfig(dataId: String, cmd: ConfigEntityCommand.Cmd): Future[Option[ConfigItem]] = {
    implicit val system = context.system
    configEntity
      .ask[ConfigReply](replyTo =>
        ShardingEnvelope(ConfigEntity.makeEntityId(namespace, dataId), ConfigEntityCommand(replyTo, cmd)))
      .map {
        case reply if IntStatus.isSuccess(reply.status) => reply.data.config
        case _                                          => None
      }
  }

  private def askConfig(
      dataId: String,
      cmd: ConfigEntityCommand.Cmd,
      onSuccess: ConfigReply.Data => ConfigResponse.Data): Future[ConfigResponse] = {
    implicit val system = context.system
    configEntity
      .ask[ConfigReply](replyTo =>
        ShardingEnvelope(ConfigEntity.makeEntityId(namespace, dataId), ConfigEntityCommand(replyTo, cmd)))
      .map {
        case value if IntStatus.isSuccess(value.status) => ConfigResponse(value.status, data = onSuccess(value.data))
        case value                                      => ConfigResponse(value.status, value.message)
      }
  }
}

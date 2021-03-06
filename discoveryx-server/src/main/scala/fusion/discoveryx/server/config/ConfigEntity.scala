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

package fusion.discoveryx.server.config

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Terminated }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model.{ ChangeType, ConfigItem, ConfigQuery, ConfigReply }
import fusion.discoveryx.server.protocol.ConfigEntityCommand.Cmd
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.HSInternalErrorException

object ConfigEntity {
  trait Command
  trait Event

  val NAME = "ConfigEntity"
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey(NAME)

  object ConfigKey {
    def unapply(entityId: String): Option[(String, String)] = entityId.split(Constants.ENTITY_ID_SEPARATOR) match {
      case Array(namespace, dataId) => Some((namespace, dataId))
      case _                        => None
    }
  }

  @inline def makeEntityId(namespace: String, dataId: String) = s"$namespace${Constants.ENTITY_ID_SEPARATOR}$dataId"

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(TypeKey)(entityContext => apply(entityContext)).withStopMessage(ConfigPassiveStop()))

  private def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    Behaviors.setup(context =>
      ConfigEntity.ConfigKey.unapply(entityContext.entityId) match {
        case Some((namespace, dataId)) =>
          new ConfigEntity(
            PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
            namespace,
            dataId,
            context).eventSourcedBehavior()
        case _ =>
          throw HSInternalErrorException(
            s"Invalid entityId, need [<namespace>${Constants.ENTITY_ID_SEPARATOR}<dataId>]，but entityId value [${entityContext.entityId}] is not [${entityContext.entityId.split(Constants.ENTITY_ID_SEPARATOR).toSeq}]")
      })
}

import fusion.discoveryx.server.config.ConfigEntity._
class ConfigEntity private (
    persistenceId: PersistenceId,
    namespace: String,
    dataId: String,
    context: ActorContext[Command]) {
  private var listeners = List.empty[ActorRef[ConfigEntity.Event]]
  private val configManager = ConfigManager.init(context.system)

  context.log.info(s"ConfigEntity started. PersistenceId is [$persistenceId].")

  def eventSourcedBehavior(): EventSourcedBehavior[Command, ChangedConfigEvent, ConfigState] = {
    val settings = ConfigSettings(context.system)
    EventSourcedBehavior[Command, ChangedConfigEvent, ConfigState](
      persistenceId,
      ConfigState.defaultInstance,
      commandHandler,
      eventHandler)
      .receiveSignal {
        case (_, Terminated(ref)) =>
          listeners = listeners.filterNot(_ == ref)
      }
      .withTagger(_ => Set(ConfigEntity.NAME, namespace))
      .withRetention(settings.retentionCriteria)
      .snapshotWhen {
        case (_, ChangedConfigEvent(_, ChangeType.CHANGE_REMOVE), _) => true
        case _                                                       => false
      }
  }

  def commandHandler(state: ConfigState, cmd: Command): Effect[ChangedConfigEvent, ConfigState] = cmd match {
    case ConfigEntityCommand(replyTo, cmd) =>
      cmd match {
        case Cmd.Get(_)      => processGet(state, replyTo)
        case Cmd.Query(in)   => processQuery(state, replyTo, in)
        case Cmd.Publish(in) => processPublish(state, replyTo, in)
        case Cmd.Remove(_)   => processRemove(replyTo)
        case Cmd.Empty       => Effect.none
      }

    case RegisterChangeListener(replyTo, _) =>
      listeners ::= replyTo
      context.watch(replyTo)
      Effect.none

    case _: ConfigPassiveStop =>
      Effect.stop()

    case _: RemoveAndStopConfigEntity =>
      Effect.persist(ChangedConfigEvent(changeType = ChangeType.CHANGE_REMOVE)).thenStop()
  }

  private def processRemove(replyTo: ActorRef[ConfigReply]): Effect[ChangedConfigEvent, ConfigState] =
    Effect
      .persist[ChangedConfigEvent, ConfigState](ChangedConfigEvent(changeType = ChangeType.CHANGE_REMOVE))
      .thenRun {
        case state if state.configItem.isEmpty => replyTo ! ConfigReply(IntStatus.OK)
        case _                                 => replyTo ! ConfigReply(IntStatus.INTERNAL_ERROR)
      }
      .thenStop()

  private def processPublish(
      state: ConfigState,
      replyTo: ActorRef[ConfigReply],
      in: ConfigItem): Effect[ChangedConfigEvent, ConfigState] = {
    if (state.configItem.contains(in)) {
      Effect.reply(replyTo)(
        ConfigReply(IntStatus.OK, "Don't need update.", ConfigReply.Data.Config(state.configItem.get)))
    } else {
      val event =
        ChangedConfigEvent(Some(in), if (state.configItem.isEmpty) ChangeType.CHANGE_ADD else ChangeType.CHANGE_SAVE)
      Effect.persist(event).thenReply(replyTo) {
        case ConfigState(Some(item)) => ConfigReply(IntStatus.OK, data = ConfigReply.Data.Config(item))
        case _                       => ConfigReply(IntStatus.BAD_REQUEST)
      }
    }
  }

  private def processGet(
      state: ConfigState,
      replyTo: ActorRef[ConfigReply]): Effect[ChangedConfigEvent, ConfigState] = {
    val data = state.configItem.map(ConfigReply.Data.Config).getOrElse(ConfigReply.Data.Empty)
    val resp = ConfigReply(if (data.isEmpty) IntStatus.NOT_FOUND else IntStatus.OK, data = data)
    Effect.reply(replyTo)(resp)
  }

  private def processQuery(
      state: ConfigState,
      replyTo: ActorRef[ConfigReply],
      in: ConfigQuery): Effect[ChangedConfigEvent, ConfigState] = {
    val data = state.configItem
      .collect {
        case item if matchGroupName(in, item) && matchTags(in, item) =>
          ConfigReply.Data.Config(item)
      }
      .getOrElse(ConfigReply.Data.Empty)
    val resp = ConfigReply(if (data.isEmpty) IntStatus.NOT_FOUND else IntStatus.OK, data = data)
    Effect.reply(replyTo)(resp)
  }

  @inline private def matchGroupName(in: ConfigQuery, item: ConfigItem): Boolean =
    in.groupName.forall(groupName => item.groupName.contains(groupName))

  @inline private def matchTags(in: ConfigQuery, item: ConfigItem): Boolean =
    in.tags.isEmpty || item.tags.exists(tag => in.tags.contains(tag))

  def eventHandler(state: ConfigState, evt: ChangedConfigEvent): ConfigState = {
    context.log.debug(s"eventHandler($state, $evt)")
    evt.changeType match {
      case ChangeType.CHANGE_REMOVE =>
        configManager ! ShardingEnvelope(namespace, ConfigRemovedEvent(dataId))
      case ChangeType.CHANGE_ADD =>
        configManager ! ShardingEnvelope(namespace, ConfigAddedEvent(dataId))
      case _ => // do nothing
    }

    listeners.foreach { ref =>
      ref ! evt
      if (evt.changeType.isChangeAdd) {
        ref ! ConfigListenerCompletedEvent()
      }
    }
    ConfigState(configItem = evt.config)
  }
}

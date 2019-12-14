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
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import fusion.discoveryx.model.{ ConfigItem, ConfigReply }
import fusion.discoveryx.server.DiscoveryPersistenceQuery
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.HSInternalErrorException

object ConfigEntity {
  trait Command
  trait ReplyCommand extends Command {
    val replyTo: ActorRef[ConfigReply]
    def withReplyTo(replyTo: ActorRef[ConfigReply]): ReplyCommand
  }
  trait Event
  trait ChangeEvent extends Event

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("configEntity")

  object ConfigKey {
    def makeEntityId(key: fusion.discoveryx.server.protocol.ConfigKey) = s"${key.namespace} ${key.dataId}"
    def makeEntityId(namespace: String, dataId: String) = s"$namespace $dataId"
    def unapply(entityId: String): Option[ConfigKey] = entityId.split(' ') match {
      case Array(namespace, dataId) => Some(new ConfigKey(namespace, dataId))
      case _                        => None
    }
  }

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(TypeKey)(entityContext => apply(entityContext)))

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    Behaviors.setup(context =>
      ConfigEntity.ConfigKey.unapply(entityContext.entityId) match {
        case Some(configKey) =>
          new ConfigEntity(
            PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
            configKey.namespace,
            configKey.dataId,
            context).eventSourcedBehavior()
        case _ =>
          throw HSInternalErrorException(
            s"Invalid entityId, need '[namespace] [dataId]'，but is ${entityContext.entityId}")
      })
}

import fusion.discoveryx.server.config.ConfigEntity._
class ConfigEntity private (
    persistenceId: PersistenceId,
    namespace: String,
    dataId: String,
    context: ActorContext[Command]) {
  private implicit val system = context.system
  private val readJournal = DiscoveryPersistenceQuery(system).readJournal

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, ConfigItem] =
    EventSourcedBehavior[Command, Event, ConfigItem](
      persistenceId,
      ConfigItem.defaultInstance,
      commandHandler,
      eventHandler).withTagger(_ => Set(ConfigEntity.TypeKey.name, namespace))

  def commandHandler(configItem: ConfigItem, cmd: Command): Effect[Event, ConfigItem] = cmd match {
    case GetConfig(_, replyTo) =>
      Effect.reply(replyTo)(ConfigReply(IntStatus.OK, data = ConfigReply.Data.Config(configItem)))

    case PublishConfig(in, replyTo) =>
      context.log.debug(s"PublishConfig($in, $replyTo)")
      val item = ConfigItem(in.namespace, in.dataId, in.groupName, in.content, in.`type`)
      val old = if (configItem == ConfigItem.defaultInstance) None else Some(configItem)
      val event = ChangedConfigEvent(item, old, if (old.isEmpty) ChangeType.CHANGE_ADD else ChangeType.CHANGE_SAVE)
      Effect.persist(event).thenReply(replyTo) {
        case `item` => ConfigReply(IntStatus.OK, data = ConfigReply.Data.Config(item))
        case _      => ConfigReply(IntStatus.BAD_REQUEST)
      }

    case RemoveConfig(_, replyTo) =>
      Effect.persist(ChangedConfigEvent(old = Some(configItem), `type` = ChangeType.CHANGE_REMOVE)).thenReply(replyTo) {
        case state if state == ConfigItem.defaultInstance => ConfigReply(IntStatus.OK)
        case _                                            => ConfigReply(IntStatus.INTERNAL_ERROR)
      }

    case RegisterChangeListener(replyTo, _) =>
      readJournal.eventsByPersistenceId(persistenceId.id, 0, Long.MaxValue).runForeach { envelope =>
        envelope.event match {
          case evt: ChangedConfigEvent => replyTo ! evt
          case other                   => context.log.debug(s"Other journal event: $other")
        }
      }
      Effect.none
  }

  def eventHandler(configItem: ConfigItem, event: Event): ConfigItem = {
    //context.log.debug(s"eventHandler($configItem, $event)")
    event match {
      // TODO How delete state ?
      case ChangedConfigEvent(item, _, _) => item
    }
  }
}

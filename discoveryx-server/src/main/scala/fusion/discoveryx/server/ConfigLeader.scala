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

package fusion.discoveryx.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.persistence.query.Offset
import akka.persistence.typed.PersistenceId
import akka.stream.scaladsl.BroadcastHub
import fusion.discoveryx.model.ChangeType
import fusion.discoveryx.server.config.{ ConfigEntity, ConfigManager }
import fusion.discoveryx.server.protocol.{
  ChangedConfigEvent,
  InternalConfigKeys,
  InternalRemoveKey,
  RemovedConfigEvent
}

import scala.concurrent.duration._

object ConfigLeader {
  val NAME = "config-leader"

  def apply(): Behavior[String] = Behaviors.setup[String] { context =>
    implicit val system = context.system
    val configManager: ActorRef[ShardingEnvelope[ConfigManager.Command]] = ConfigManager.init(system)
    val readJournal = DiscoveryPersistenceQuery(system).readJournal
    readJournal
      .persistenceIds()
      .mapConcat { persistenceId =>
        persistenceId.split("\\" + PersistenceId.DefaultSeparator) match {
          case Array(typeName, ConfigEntity.ConfigKey(configKey)) if typeName == ConfigEntity.TypeKey.name =>
            configKey :: Nil
          case _ => Nil
        }
      }
      .groupedWithin(2048, 1.seconds)
      .filter(_.nonEmpty)
      .runForeach { list =>
        for ((namespace, keys) <- list.groupBy(_.namespace)) {
          configManager ! ShardingEnvelope(namespace, InternalConfigKeys(keys))
        }
      }
    val eventSource = readJournal
      .eventsByTag(ConfigEntity.TypeKey.name, Offset.noOffset)
      .mapConcat { envelop =>
        envelop.persistenceId.split("\\" + PersistenceId.DefaultSeparator) match {
          case Array(typeName, ConfigEntity.ConfigKey(configKey)) if typeName == ConfigEntity.TypeKey.name =>
            (configKey, envelop) :: Nil
          case _ => Nil
        }
      }
      .runWith(BroadcastHub.sink)

    eventSource.runForeach {
      case (configKey, envelope) =>
        envelope.event match {
          case event: ChangedConfigEvent if event.`type` == ChangeType.CHANGE_REMOVE => // TODO ?
            configManager ! ShardingEnvelope(configKey.namespace, InternalRemoveKey(configKey))
          case _: RemovedConfigEvent => // do nothing
            configManager ! ShardingEnvelope(configKey.namespace, InternalRemoveKey(configKey))
        }
    }

    Behaviors.ignore
  }
}

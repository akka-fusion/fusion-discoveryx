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

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Terminated }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import fusion.discoveryx.server.protocol.ConfigManagerCommand.Cmd
import fusion.discoveryx.server.protocol.{ ConfigManagerCommand, ConfigResponse }
import helloscala.common.IntStatus

object ConfigManager {
  trait Command

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ConfigManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(TypeKey)(entityContext => apply(entityContext.entityId)))

  def apply(entityId: String): Behavior[Command] =
    Behaviors.setup(context => new ConfigManager(entityId, context).init())
}

import fusion.discoveryx.server.config.ConfigManager._
class ConfigManager private (namespace: String, context: ActorContext[Command]) {
  private var dataIds = Vector.empty[ActorRef[ConfigEntity.Command]]

  def init(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case ConfigManagerCommand(replyTo, cmd) => onManagerCommand(cmd, replyTo)
      }
      .receiveSignal {
        case (_, Terminated(ref)) =>
          dataIds = dataIds.filterNot(_ == ref)
          Behaviors.same
      }

  private def onManagerCommand(cmd: ConfigManagerCommand.Cmd, replyTo: ActorRef[ConfigResponse]): Behavior[Command] = {
    cmd match {
      case Cmd.List(cmd)    =>
      case Cmd.Get(cmd)     =>
      case Cmd.Publish(cmd) =>
      case Cmd.Remove(cmd)  =>
      case Cmd.Empty        => replyTo ! ConfigResponse(IntStatus.BAD_REQUEST, "Invalid command.")
    }
    Behaviors.same
  }
}

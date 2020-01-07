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

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import fusion.discoveryx.server.naming.internal.NamingManagerBehavior

import scala.concurrent.duration._

object NamingManager {
  trait Command extends NamingService.Command
  trait Event

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("NamingManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
    ClusterSharding(system).init(
      Entity(TypeKey)(entityContext => NamingManager(entityContext.entityId))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))
  }

  private def apply(namespace: String): Behavior[Command] =
    Behaviors.setup(context => new NamingManagerBehavior(namespace, context).eventSourcedBehavior())
}

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

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.persistence.typed.PersistenceId
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model._
import helloscala.common.exception.HSBadRequestException
import helloscala.common.util.StringUtils

import scala.concurrent.duration._

object NamingService {
  trait Command
//  private[naming] final case class InternalInstanceModified(reply: NamingReply, replyTo: ActorRef[NamingReply])
//      extends Command
  private[naming] final case class InternalHealthyChanged(inst: Instance, healthy: Boolean) extends Command

  trait Event

  object ServiceKey {
    def unapply(entityId: String): Option[ServiceItem] = entityId.split(' ') match {
      case Array(namespace, serviceName) =>
        Some(new ServiceItem(namespace, serviceName, Constants.DEFAULT_GROUP_NAME))
      case _ => None
    }
  }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("NamingService")

  def entityId(namespace: String, serviceName: String): Either[String, String] = {
    if (StringUtils.isBlank(namespace) || StringUtils.isBlank(serviceName))
      Left("entityId invalid, need '[namespace] [serviceName]' format.")
    else Right(s"$namespace $serviceName")
  }

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(NamingService.TypeKey)(entityContext => apply(entityContext))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))

  private def apply(entityContext: EntityContext[Command]): Behavior[Command] = Behaviors.setup[Command] { context =>
    val ServiceItem = ServiceKey
      .unapply(entityContext.entityId)
      .getOrElse(throw HSBadRequestException(
        s"${context.self} create child error. entityId invalid, need '[namespace] [serviceName]' format."))
    Behaviors.withTimers(
      timers =>
        new actors.NamingServiceBehavior(ServiceItem, timers, context)
          .eventSourcedBehavior(PersistenceId.of(entityContext.entityTypeKey.name, entityContext.entityId)))
  }
}

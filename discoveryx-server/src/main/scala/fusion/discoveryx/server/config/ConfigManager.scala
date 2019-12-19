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

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.util.Timeout
import fusion.discoveryx.model._
import fusion.discoveryx.server.protocol.ConfigManagerCommand.Cmd
import fusion.discoveryx.server.protocol.ConfigResponse.Data
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object ConfigManager {
  trait Command

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ConfigManager")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(TypeKey)(entityContext => apply(entityContext.entityId))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))

  def apply(entityId: String): Behavior[Command] =
    Behaviors.setup(context => new ConfigManager(entityId, context).init())
}

import fusion.discoveryx.server.config.ConfigManager._
class ConfigManager private (namespace: String, context: ActorContext[Command]) {
  import context.executionContext
  private implicit val system = context.system
  private val settings = ConfigSettings(context.system)
  private val configEntity = ConfigEntity.init(system)
  private var configKeys = Vector.empty[ConfigKey]

  def init(): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case ConfigManagerCommand(replyTo, cmd) =>
        onManagerCommand(cmd).onComplete {
          case Success(value) => replyTo ! value
          case Failure(e)     => replyTo ! ConfigResponse(IntStatus.INTERNAL_ERROR, e.getMessage)
        }
        Behaviors.same
      case InternalConfigKeys(keys) =>
        for (key <- keys if !configKeys.contains(key)) {
          configKeys :+= key
        }
        Behaviors.same
      case InternalRemoveKey(key) =>
        configKeys = configKeys.filterNot(_ == key)
        Behaviors.same
    }

  implicit val timeout: Timeout = 5.seconds

  private def onManagerCommand(command: Cmd): Future[ConfigResponse] = command match {
    case Cmd.List(cmd) => processList(cmd)
    case Cmd.Get(cmd) =>
      askConfig(cmd.dataId, ConfigEntityCommand.Cmd.Get(cmd), _.config.map(Data.Config).getOrElse(Data.Empty))
    case Cmd.Publish(cmd) =>
      askConfig(
        cmd.dataId,
        ConfigEntityCommand.Cmd.Publish(cmd),
        _.config
          .map { item =>
            context.self ! InternalConfigKeys(ConfigKey(item.namespace, item.dataId) :: Nil)
            Data.Config(item)
          }
          .getOrElse(Data.Empty))
    case Cmd.Remove(cmd) => askConfig(cmd.dataId, ConfigEntityCommand.Cmd.Remove(cmd), _ => Data.Empty)
    case Cmd.Empty       => Future.successful(ConfigResponse(IntStatus.BAD_REQUEST, "Invalid command."))
  }

  private def processList(cmd: ListConfig): Future[ConfigResponse] = {
    val page = settings.findPage(cmd.page)
    val size = settings.findPage(cmd.size)
    val offset = settings.findOffset(page, size)
    context.log.info(s"dataIds: $configKeys")
    if (offset < configKeys.size) {
      val futures = configKeys.view
        .slice(offset, offset + size)
        .map(configKey => askConfig(configKey, ConfigEntityCommand.Cmd.Get(ConfigGet(cmd.namespace))))
        .toVector
      Future.sequence(futures).map { replies =>
        val configs = replies.collect { case Some(item) => itemToBasic(item) }
        ConfigResponse(IntStatus.OK, data = Data.Listed(ConfigQueried(configs, namespace, page, size, configKeys.size)))
      }
    } else {
      Future.successful(
        ConfigResponse(
          IntStatus.OK,
          data = Data.Listed(
            ConfigQueried(namespace = namespace, page = page, size = size, totalElements = configKeys.size))))
    }
  }

  private def itemToBasic(item: ConfigItem) = ConfigBasic(item.dataId, item.groupName, item.`type`)

  private def askConfig(configKey: ConfigKey, cmd: ConfigEntityCommand.Cmd): Future[Option[ConfigItem]] = {
    configEntity
      .ask[ConfigReply](replyTo =>
        ShardingEnvelope(ConfigEntity.makeEntityId(configKey), ConfigEntityCommand(replyTo, cmd)))
      .map {
        case reply if IntStatus.isSuccess(reply.status) => reply.data.config
        case _                                          => None
      }
  }

  private def askConfig(
      dataId: String,
      cmd: ConfigEntityCommand.Cmd,
      onSuccess: ConfigReply.Data => ConfigResponse.Data): Future[ConfigResponse] = {
    configEntity
      .ask[ConfigReply](replyTo =>
        ShardingEnvelope(ConfigEntity.makeEntityId(namespace, dataId), ConfigEntityCommand(replyTo, cmd)))
      .map {
        case value if IntStatus.isSuccess(value.status) => ConfigResponse(value.status, data = onSuccess(value.data))
        case value                                      => ConfigResponse(value.status, value.message)
      }
  }
}

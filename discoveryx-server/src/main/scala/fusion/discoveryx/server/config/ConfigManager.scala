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
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, PostStop }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.model._
import fusion.discoveryx.server.management.Management
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

  private def apply(entityId: String): Behavior[Command] =
    Behaviors.setup(context => new ConfigManager(entityId, context).receive())
}

import fusion.discoveryx.server.config.ConfigManager._
class ConfigManager private (namespace: String, context: ActorContext[Command]) {
  import context.executionContext
  private implicit val system = context.system
  private val settings = ConfigSettings(context.system)
  private val configEntity = ConfigEntity.init(system)
  private val managementRef = Management.init(system)
  private var configKeys = Vector.empty[ConfigKey]

  def receive(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case ConfigManagerCommand(replyTo, cmd) =>
          onManagerCommand(cmd).onComplete {
            case Success(value) => replyTo ! value
            case Failure(e)     => replyTo ! ConfigResponse(IntStatus.INTERNAL_ERROR, e.getMessage)
          }
          Behaviors.same
        case InternalConfigKeys(keys) =>
          for (key <- keys if !configKeys.contains(key)) {
            saveConfigKeys(configKeys :+ key)
          }
          Behaviors.same
        case InternalRemoveKey(key) =>
          saveConfigKeys(configKeys.filterNot(_ == key))
          Behaviors.same
        case _: StopConfigManager =>
          Behaviors.stopped
      }
      .receiveSignal {
        case (_, PostStop) =>
          cleanup()
          Behaviors.same
      }

  private def cleanup(): Unit = {
    for (key <- configKeys) {
      configEntity ! ShardingEnvelope(ConfigEntity.makeEntityId(key), RemoveAndStopConfigEntity())
    }
  }

  private def saveConfigKeys(keys: Vector[ConfigKey]): Unit = {
    configKeys = keys
    managementRef ! ConfigSizeChangeEvent(namespace, configKeys.size)
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
    val (page, size, offset) = settings.generatePageSizeOffset(cmd.page, cmd.size)
    if (offset < configKeys.size) {
      Source(configKeys)
        .filter(key => cmd.dataId.forall(dataId => key.dataId.contains(dataId)))
        .mapAsync(math.min(8, size)) { configKey =>
          askConfig(configKey, ConfigEntityCommand.Cmd.Query(ConfigQuery(cmd.groupName, cmd.tags)))
        }
        .collect { case Some(item) => itemToBasic(item) }
        .drop(offset)
        .take(size)
        .runWith(Sink.seq)
        .map { configs =>
          ConfigResponse(
            IntStatus.OK,
            data = Data.Listed(ConfigQueried(configs, namespace, page, size, configKeys.size)))
        }
    } else {
      Future.successful(
        ConfigResponse(
          IntStatus.OK,
          s"offset: $offset, but ConfigEntity size is ${configKeys.size}",
          Data.Listed(ConfigQueried(Nil, namespace, page, size, configKeys.size))))
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

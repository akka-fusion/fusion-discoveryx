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

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import fusion.discoveryx.grpc.ConfigService
import fusion.discoveryx.model._
import fusion.discoveryx.server.management.NamespaceRef
import fusion.discoveryx.server.management.NamespaceRef.{ ExistNamespace, NamespaceExists }
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.HSInternalErrorException
import helloscala.common.util.Utils

import scala.concurrent.Future
import scala.concurrent.duration._

class ConfigServiceImpl(namespaceRef: ActorRef[NamespaceRef.ExistNamespace])(implicit system: ActorSystem[_])
    extends ConfigService {
  implicit private val timeout: Timeout = 5.seconds
  private val configEntity = ConfigEntity.init(system)

  override def serverStatus(in: ServerStatusQuery): Future[ServerStatusBO] =
    Future.successful(ServerStatusBO(IntStatus.OK))

  override def getConfig(in: ConfigGet): Future[ConfigReply] =
    askConfig(in.namespace, in.dataId, ConfigEntityCommand.Cmd.Get(in))

  override def publishConfig(in: ConfigItem): Future[ConfigReply] =
    askConfig(in.namespace, in.dataId, ConfigEntityCommand.Cmd.Publish(in))

  override def removeConfig(in: ConfigRemove): Future[ConfigReply] =
    askConfig(in.namespace, in.dataId, ConfigEntityCommand.Cmd.Remove(in))

  override def listenerConfig(in: ConfigChangeListen): Source[ConfigChanged, NotUsed] = {
    val entityId = ConfigEntity.makeEntityId(in.namespace, in.dataId)
    val (ref, source) = ActorSource
      .actorRef[ConfigEntity.Event](
        { case _: RemovedConfigEvent => },
        changed => throw HSInternalErrorException(s"Throw error: $changed."),
        2,
        OverflowStrategy.dropHead)
      .preMaterialize()
    configEntity ! ShardingEnvelope(entityId, RegisterChangeListener(ref, Utils.timeBasedUuid().toString))
    source
      .mapConcat {
        case event: ChangedConfigEvent => event :: Nil
        case _                         => Nil
      }
      .map(event => ConfigChanged(event.config, changeType = event.`type`))
  }

  @inline private def askConfig(namespace: String, dataId: String, cmd: ConfigEntityCommand.Cmd): Future[ConfigReply] =
    namespaceRef
      .ask[NamespaceExists](replyTo => ExistNamespace(namespace, replyTo))
      .flatMap {
        case NamespaceExists(true) =>
          configEntity.ask[ConfigReply](replyTo => ShardingEnvelope(namespace, ConfigEntityCommand(replyTo, cmd)))
        case _ =>
          Future.successful(ConfigReply(IntStatus.BAD_REQUEST, s"Namespace '$namespace' not exists."))
      }(system.executionContext)
}

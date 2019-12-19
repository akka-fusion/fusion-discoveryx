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
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.server.grpc.ConfigManagerService
import fusion.discoveryx.server.management.NamespaceRef
import fusion.discoveryx.server.management.NamespaceRef.NamespaceExists
import fusion.discoveryx.server.protocol.ConfigManagerCommand.Cmd
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._

class ConfigManagerServiceImpl(namespaceRef: ActorRef[NamespaceRef.ExistNamespace])(implicit system: ActorSystem[_])
    extends ConfigManagerService
    with StrictLogging {
  private implicit val timeout: Timeout = 10.seconds
  private val configManager: ActorRef[ShardingEnvelope[ConfigManager.Command]] = ConfigManager.init(system)

  /**
   * #ListConfig
   * 查询配置列表（不返回配置内容）
   */
  override def listConfig(in: ListConfig): Future[ConfigResponse] = askConfig(in.namespace, Cmd.List(in))

  @inline private def askConfig(namespace: String, cmd: ConfigManagerCommand.Cmd): Future[ConfigResponse] =
    namespaceRef
      .ask[NamespaceExists](replyTo => NamespaceRef.ExistNamespace(namespace, replyTo))
      .flatMap {
        case NamespaceExists(true) =>
          configManager.ask[ConfigResponse](replyTo => ShardingEnvelope(namespace, ConfigManagerCommand(replyTo, cmd)))
        case _ =>
          Future.successful(ConfigResponse(IntStatus.BAD_REQUEST, s"Namespace '$namespace' not exists."))
      }(system.executionContext)
}

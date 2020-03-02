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

package fusion.discoveryx.client.impl

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import fusion.core.extension.FusionCore
import fusion.discoveryx.client.{ ConfigClient, ConfigClientSettings }
import fusion.discoveryx.grpc.ConfigServiceClient
import fusion.discoveryx.model._

import scala.concurrent.Future

private[client] class ConfigClientImpl(val settings: ConfigClientSettings, val configClient: ConfigServiceClient)(
    implicit system: ActorSystem[_])
    extends ConfigClient
    with StrictLogging {
  FusionCore(system).shutdowns.beforeServiceUnbind("ConfigClient") { () =>
    configClient.close()
  }
  logger.info(s"ConfigClient was instanced, setting is [$settings], class is [$getClass].")

  override def serverStatus(in: ServerStatusQuery): Future[ServerStatusBO] = configClient.serverStatus(in)

  override def getConfig(in: ConfigGet): Future[ConfigReply] = configClient.getConfig(in)

  override def publishConfig(in: ConfigItem): Future[ConfigReply] = configClient.publishConfig(in)

  override def removeConfig(in: ConfigRemove): Future[ConfigReply] = configClient.removeConfig(in)

  override def listenerConfig(in: ConfigChangeListen): Source[ConfigChanged, NotUsed] = configClient.listenerConfig(in)
}

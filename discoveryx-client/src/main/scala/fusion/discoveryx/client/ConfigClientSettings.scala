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

package fusion.discoveryx.client

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import fusion.discoveryx.common.Constants
import helloscala.common.Configuration

object ConfigClientSettings {
  def apply(system: ActorSystem[_]): ConfigClientSettings = fromConfig(system.settings.config)
  def fromConfig(config: Config, prefix: String = s"${Constants.DISCOVERYX}.client.config"): ConfigClientSettings =
    fromConfiguration(Configuration(config), prefix)
  def fromConfiguration(
      config: Configuration,
      prefix: String = s"${Constants.DISCOVERYX}.client.config"): ConfigClientSettings =
    new ConfigClientSettings(config.getConfiguration(prefix))
}
final class ConfigClientSettings private (c: Configuration) {
  val namespace: Option[String] = c.get[Option[String]]("namespace")
  val dataId: Option[String] = c.get[Option[String]]("data-id") orElse c.get[Option[String]]("dataId")
  override def toString: String = c.underlying.root().toString
}

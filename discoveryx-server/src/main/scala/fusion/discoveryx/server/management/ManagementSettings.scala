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

package fusion.discoveryx.server.management

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.naming.BaseSettings
import helloscala.common.Configuration

class ManagementSettings(configuration: Configuration) extends BaseSettings {
  val c: Configuration = configuration.getConfiguration(s"${Constants.DISCOVERYX}.server.management")
  val enable: Boolean = c.getBoolean("enable")
}

object ManagementSettings {
  def apply(system: ActorSystem[_]): ManagementSettings = apply(system.settings.config)
  def apply(config: Config): ManagementSettings = new ManagementSettings(Configuration(config))
}

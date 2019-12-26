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
import fusion.discoveryx.server.{ BaseSettings, RetentionCriteriaSettings }
import helloscala.common.Configuration

import scala.concurrent.duration.FiniteDuration

class ManagementSettings(configuration: Configuration) extends BaseSettings with RetentionCriteriaSettings {
  val c: Configuration = configuration.getConfiguration(s"${Constants.DISCOVERYX}.server.management")
  lazy val sessionTimeout: Long = c.get[FiniteDuration]("session-timeout").toMillis
}

object ManagementSettings {
  def apply(system: ActorSystem[_]): ManagementSettings = apply(system.settings.config)
  def apply(config: Config): ManagementSettings = new ManagementSettings(Configuration(config))
}

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

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

object NamingClientSettings {
  def apply(system: ActorSystem[_]): NamingClientSettings = fromConfig(system.settings.config)
  def fromConfig(config: Config, prefix: String = s"${Constants.DISCOVERYX}.client.naming"): NamingClientSettings =
    fromConfiguration(Configuration(config), prefix)
  def fromConfiguration(
      config: Configuration,
      prefix: String = s"${Constants.DISCOVERYX}.client.naming"): NamingClientSettings =
    new NamingClientSettings(config.getConfiguration(prefix))
}

final class NamingClientSettings private (val c: Configuration) {
  val heartbeatInterval: FiniteDuration = c.getDuration("heartbeat-interval").toScala
  val oneHealthy: Boolean = c.getBoolean("one-healthy")
  val allHealthy: Boolean = c.getBoolean("all-healthy")
  val namespace: Option[String] = c.get[Option[String]]("namespace")
  val serviceName: Option[String] = c.get[Option[String]]("service-name") orElse c.get[Option[String]]("serviceName")
  val groupName: Option[String] = c.get[Option[String]]("group-name") orElse c.get[Option[String]]("groupName")
  val ip: Option[String] = c.get[Option[String]]("ip")
  val port: Option[Int] = c.get[Option[Int]]("port")
  val enable: Boolean = c.getOrElse("enable", true)
  val weight: Double = c.getOrElse("weight", 1.0)
  val ephemeral: Boolean = c.getOrElse("ephemeral", false)
  val metadata: Map[String, String] =
    if (c.hasPath("metadata")) c.get[Map[String, String]]("metadata") else Map()
  override def toString: String = c.underlying.root().toString
}

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
import fusion.discoveryx.model.{ HealthyCheckMethod, HealthyCheckProtocol }
import helloscala.common.Configuration

import scala.concurrent.duration._
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
  val autoRegistration: Boolean = c.getOrElse("auto-registration", false)
  val heartbeatInterval: FiniteDuration = c.getDuration("heartbeat-interval").toScala
  val oneHealthy: Boolean = c.getBoolean("one-healthy")
  val allHealthy: Boolean = c.getBoolean("all-healthy")
  val namespace: Option[String] = c.get[Option[String]]("namespace")
  val serviceName: Option[String] = c.get[Option[String]]("service-name") orElse c.get[Option[String]]("serviceName")
  val groupName: Option[String] = c.get[Option[String]]("group-name") orElse c.get[Option[String]]("groupName")
  val ip: Option[String] = c.get[Option[String]]("ip")
  val port: Option[Int] = c.get[Option[Int]]("port")
  val enable: Boolean = c.getOrElse("enable", true)
  val health: Boolean = c.getOrElse("health", true)
  val weight: Double = c.getOrElse("weight", 1.0)
  val ephemeral: Boolean = c.getOrElse("ephemeral", false)
  val metadata: Map[String, String] =
    if (c.hasPath("metadata")) c.get[Map[String, String]]("metadata") else Map()
  val healthyCheckMethod: HealthyCheckMethod = c
    .get[Option[String]]("healthy-check-method")
    .flatMap(name => HealthyCheckMethod.fromName(name))
    .orElse(c.get[Option[Int]]("healthy-check-method").map(id => HealthyCheckMethod.fromValue(id)))
    .filterNot(v => v.isUnrecognized || v.isNotSet)
    .getOrElse(HealthyCheckMethod.CLIENT_REPORT)
  val healthyCheckInterval: Int = c.getOrElse("healthy-check-interval", 10)
  val unhealthyCheckCount: Int = {
    val count = c.getOrElse("unhealthy-check-count", 1)
    if (count < 1) 1 else count
  }
  val protocol: HealthyCheckProtocol = c
    .get[Option[String]]("protocol")
    .flatMap(HealthyCheckProtocol.fromName)
    .orElse(
      c.get[Option[Int]]("protocol")
        .map(HealthyCheckProtocol.fromValue)
        .filterNot(v => v.isUnrecognized || v.isUnknown))
    .getOrElse(HealthyCheckProtocol.HTTP)
  val useTls: Boolean = c.getOrElse("use-tls", false)
  val httpPath: String = c.getOrElse("http-path", "")
  val queryTimeout: FiniteDuration = c.getOrElse("query-timeout", 5.seconds)
  override def toString: String = c.underlying.root().render()
}

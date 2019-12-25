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

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source
import fusion.discoveryx.client.impl.ConfigClientImpl
import fusion.discoveryx.grpc.{ ConfigService, ConfigServiceClient }
import fusion.discoveryx.model._

import scala.concurrent.Future

trait ConfigClient {
  val configClient: ConfigServiceClient

  /**
   * 查询服务状态
   */
  def serverStatus(in: ServerStatusQuery): Future[ServerStatusBO]

  /**
   * 查询配置
   */
  def getConfig(in: ConfigGet): Future[ConfigReply]

  /**
   * 发布配置
   */
  def publishConfig(in: ConfigItem): Future[ConfigReply]

  /**
   * 删除配置
   */
  def removeConfig(in: ConfigRemove): Future[ConfigReply]

  /**
   * 监听配置变化
   */
  def listenerConfig(in: ConfigChangeListen): Source[ConfigChanged, NotUsed]
}

object ConfigClient {
  def apply(system: ActorSystem[_]): ConfigClient = {
    apply(ConfigClientSettings(system), system)
  }

  // Java API
  def create(system: ActorSystem[_]): ConfigClient = apply(system)

  def apply(settings: ConfigClientSettings, system: ActorSystem[_]): ConfigClient = {
    implicit val classicSystem = system.toClassic
    import system.executionContext
    apply(settings, ConfigServiceClient(GrpcClientSettings.fromConfig(ConfigService.name)))(system)
  }

  // Java API
  def create(settings: ConfigClientSettings, system: ActorSystem[_]): ConfigClient = apply(settings, system)

  def apply(settings: ConfigClientSettings, serviceClient: ConfigServiceClient)(
      implicit system: ActorSystem[_]): ConfigClient =
    new ConfigClientImpl(settings, serviceClient)(system)

  // Java API
  def create(settings: ConfigClientSettings, serviceClient: ConfigServiceClient, system: ActorSystem[_]): ConfigClient =
    apply(settings, serviceClient)(system)
}

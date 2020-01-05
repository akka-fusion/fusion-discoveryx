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

import akka.actor.typed.{ ActorSystem, Extension }
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.model.Uri
import fusion.discoveryx.client.impl.NamingClientImpl
import fusion.discoveryx.grpc.{ NamingService, NamingServiceClient }
import fusion.discoveryx.model._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait NamingClient extends Extension {
  val settings: NamingClientSettings
  val client: NamingServiceClient

  /**
   * 查询服务状态
   */
  def serverStatus(in: ServerStatusQuery): Future[ServerStatusBO]

  /**
   * 通过默认配置注册实例
   */
  def registerOnSettings(): Future[NamingReply]

  /**
   * 添加实例
   */
  def registerInstance(in: InstanceRegister): Future[NamingReply]

  /**
   * 添加实例
   * @param delay 注册实例重试间隔（当实例注册成功开始心跳连接后，若心跳连接断开也将重试）
   */
  def registerInstance(in: InstanceRegister, delay: FiniteDuration): Future[NamingReply]

  /**
   * 修改实例
   */
  def modifyInstance(in: InstanceModify): Future[NamingReply]

  /**
   * 删除实例
   */
  def removeInstance(in: InstanceRemove): Future[NamingReply]

  /**
   * 查询实例
   */
  def queryInstance(in: InstanceQuery): Future[NamingReply]

  def oneHealthyInstance(serviceName: String): Future[Option[Instance]]

  def oneHealthyInstance(namespace: String, serviceName: String): Future[Option[Instance]]

  def generateUri(uri: Uri): Future[Option[Uri]]
  def generateUri(uri: akka.http.javadsl.model.Uri): Future[Option[akka.http.javadsl.model.Uri]]
}

object NamingClient {
  case class InstanceKey( /*namespace: String, serviceName: String, */ instanceId: String)
  object InstanceKey {
    def apply(in: Instance): InstanceKey = InstanceKey( /*in.namespace, in.serviceName,*/ in.instanceId)
    def apply(in: InstanceRemove): InstanceKey = InstanceKey( /*in.namespace, in.serviceName,*/ in.instanceId)
  }

  def apply(system: ActorSystem[_]): NamingClient = {
    apply(NamingClientSettings(system), system)
  }

  // Java API
  def create(system: ActorSystem[_]): NamingClient = apply(system)

  def apply(settings: NamingClientSettings, system: ActorSystem[_]): NamingClient = {
    implicit val classicSystem = system.toClassic
    import system.executionContext
    val grpcClientSettings = GrpcClientSettings.fromConfig(NamingService.name)
    apply(settings, NamingServiceClient(grpcClientSettings))(system)
  }

  // Java API
  def create(settings: NamingClientSettings, system: ActorSystem[_]): NamingClient = apply(settings, system)

  def apply(settings: NamingClientSettings, serviceClient: NamingServiceClient)(
      implicit system: ActorSystem[_]): NamingClient =
    new NamingClientImpl(settings, serviceClient)(system)

  // Java API
  def create(settings: NamingClientSettings, serviceClient: NamingServiceClient, system: ActorSystem[_]): NamingClient =
    apply(settings, serviceClient)(system)
}

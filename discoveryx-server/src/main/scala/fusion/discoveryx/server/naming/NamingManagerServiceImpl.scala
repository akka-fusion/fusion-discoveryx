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

package fusion.discoveryx.server.naming

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.util.Timeout
import fusion.discoveryx.model.{ InstanceModify, InstanceRegister, InstanceRemove }
import fusion.discoveryx.server.grpc.NamingManagerService
import fusion.discoveryx.server.protocol.NamingManagerCommand.Cmd
import fusion.discoveryx.server.protocol.{ GetService, ListService, NamingManagerCommand, NamingResponse }

import scala.concurrent.Future
import scala.concurrent.duration._

class NamingManagerServiceImpl()(implicit system: ActorSystem[_]) extends NamingManagerService {
  private implicit val timeout: Timeout = 10.seconds
  private val namingManager: ActorRef[ShardingEnvelope[NamingManager.Command]] = NamingManager.init(system)

  /**
   * #CreateInstance
   * 创建实例
   */
  override def createInstance(in: InstanceRegister): Future[NamingResponse] =
    askManager(in.namespace, Cmd.InstanceCreate(in))

  /**
   * #ListService
   * 查询服务列表
   */
  override def listService(in: ListService): Future[NamingResponse] = askManager(in.namespace, Cmd.ListService(in))

  /**
   * #GetService
   * 查询单个服务
   */
  override def getService(in: GetService): Future[NamingResponse] = askManager(in.namespace, Cmd.GetService(in))

  /**
   * #RemoveInstance
   * 删除实例
   */
  override def removeInstance(in: InstanceRemove): Future[NamingResponse] =
    askManager(in.namespace, Cmd.InstanceRemove(in))

  /**
   * #ModifyInstance
   * 编辑实例
   */
  override def modifyInstance(in: InstanceModify): Future[NamingResponse] =
    askManager(in.namespace, Cmd.InstanceModify(in))

  @inline private def askManager(namespace: String, cmd: NamingManagerCommand.Cmd): Future[NamingResponse] =
    namingManager.ask[NamingResponse](replyTo => ShardingEnvelope(namespace, NamingManagerCommand(replyTo, cmd)))
}

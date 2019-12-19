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
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import fusion.discoveryx.server.grpc.ManagementService
import fusion.discoveryx.server.protocol.ManagementCommand.Cmd
import fusion.discoveryx.server.protocol._

import scala.concurrent.Future
import scala.concurrent.duration._

class ManagementServiceImpl()(implicit system: ActorSystem[_]) extends ManagementService {
  implicit private val timeout: Timeout = 5.seconds
  private val managementRef = Management.init(system)

  /**
   * #ListNamespace
   * 创建命名空间
   */
  override def listNamespace(in: ListNamespace): Future[ManagementResponse] = askCommand(Cmd.List(in))

  /**
   * #CreateNamespace
   * 创建命名空间
   */
  override def createNamespace(in: CreateNamespace): Future[ManagementResponse] = askCommand(Cmd.Create(in))

  /**
   * #ModifyNamespace
   * 修改命名空间
   */
  override def modifyNamespace(in: ModifyNamespace): Future[ManagementResponse] = askCommand(Cmd.Modify(in))

  /**
   * #RemoveNamespace
   * 删除命名空间
   */
  override def removeNamespace(in: RemoveNamespace): Future[ManagementResponse] = askCommand(Cmd.Remove(in))

  @inline private def askCommand(cmd: Cmd): Future[ManagementResponse] =
    managementRef.ask[ManagementResponse](replyTo => ManagementCommand(replyTo, cmd))
}

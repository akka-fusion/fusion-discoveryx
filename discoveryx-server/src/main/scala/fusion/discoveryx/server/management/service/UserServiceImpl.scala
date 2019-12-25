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

package fusion.discoveryx.server.management.service

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import fusion.discoveryx.server.grpc.UserService
import fusion.discoveryx.server.management.{ UserEntity, UserManager }
import fusion.discoveryx.server.protocol.UserCommand.Cmd
import fusion.discoveryx.server.protocol._

import scala.concurrent.Future
import scala.concurrent.duration._

class UserServiceImpl()(implicit system: ActorSystem[_]) extends UserService {
  implicit private val timeout: Timeout = 5.seconds
  private val userEntity = UserEntity.init(system)
  private val userManager = UserManager.init(system)

  override def login(in: Login): Future[UserResponse] =
    userEntity.ask[UserResponse](replyTo => ShardingEnvelope(in.account, UserCommand(replyTo, Cmd.Login(in))))

  override def logout(in: Logout): Future[UserResponse] =
    askUserEntity(in.account, Cmd.Logout(in))

  override def createUser(in: CreateUser): Future[UserResponse] =
    askUserEntity(in.account, Cmd.Create(in))

  override def modifyUser(in: ModifyUser): Future[UserResponse] =
    askUserEntity(in.account, Cmd.Modify(in))

  override def removeUser(in: RemoveUser): Future[UserResponse] =
    askUserEntity(in.account, Cmd.Remove(in))

  override def listUser(in: ListUser): Future[UserResponse] =
    userManager.ask[UserResponse](replyTo => UserManagerCommand(replyTo, UserManagerCommand.Cmd.List(in)))

  @inline private def askUserEntity(account: String, cmd: Cmd): Future[UserResponse] =
    userEntity.ask[UserResponse](replyTo => ShardingEnvelope(account, UserCommand(replyTo, cmd)))
}

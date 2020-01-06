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

package fusion.discoveryx.server.user.service

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import fusion.discoveryx.server.grpc.UserService
import fusion.discoveryx.server.user.UserEntity
import fusion.discoveryx.server.protocol.UserCommand.Cmd
import fusion.discoveryx.server.protocol._
import fusion.discoveryx.server.util.SessionUtils
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._

class UserServiceImpl(
    userEntity: ActorRef[ShardingEnvelope[UserEntity.Command]],
    userManager: ActorRef[UserEntity.Command])(implicit system: ActorSystem[_])
    extends UserService {
  implicit private val timeout: Timeout = 5.seconds

  override def login(in: Login): Future[UserResponse] = askUserEntity(in.account, Cmd.Login(in))

  override def logout(in: Logout): Future[UserResponse] = askUserEntity(in.account, Cmd.Logout(in))

  override def createUser(in: CreateUser): Future[UserResponse] = askUserEntity(in.account, Cmd.Create(in))

  override def modifyUser(in: ModifyUser): Future[UserResponse] = askUserEntity(in.account, Cmd.Modify(in))

  override def removeUser(in: RemoveUser): Future[UserResponse] = askUserEntity(in.account, Cmd.Remove(in))

  override def listUser(in: ListUser): Future[UserResponse] =
    userManager.ask[UserResponse](replyTo => UserManagerCommand(replyTo, UserManagerCommand.Cmd.List(in)))

  override def getUser(in: GetUser): Future[UserResponse] = askUserEntity(in.account, Cmd.Get(in))

  override def currentSessionUser(in: CurrentSessionUser): Future[UserResponse] = {
    SessionUtils.parseAccount(in.token) match {
      case Right(value)  => askUserEntity(value.account, Cmd.TokenAccount(value))
      case Left(message) => Future.successful(UserResponse(IntStatus.UNAUTHORIZED, message))
    }
  }

  @inline private def askUserEntity(account: String, cmd: Cmd): Future[UserResponse] =
    UserEntity.validationAccount(account) match {
      case Right(_)  => userEntity.ask[UserResponse](replyTo => ShardingEnvelope(account, UserCommand(replyTo, cmd)))
      case Left(msg) => Future.successful(UserResponse(IntStatus.BAD_REQUEST, msg))
    }
}

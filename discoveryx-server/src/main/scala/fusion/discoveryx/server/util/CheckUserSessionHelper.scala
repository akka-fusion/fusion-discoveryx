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

package fusion.discoveryx.server.util

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.grpc.scaladsl.Metadata
import akka.http.scaladsl.model.headers.Cookie
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.user.UserEntity
import fusion.discoveryx.server.protocol.UserCommand.Cmd
import fusion.discoveryx.server.protocol.{ CheckSession, TokenAccount, UserCommand, UserResponse }
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.util.control.NonFatal

class CheckUserSessionHelper(userEntity: ActorRef[ShardingEnvelope[UserEntity.Command]])(
    implicit system: ActorSystem[_],
    timeout: Timeout)
    extends StrictLogging {
  private def checkUserSession[RESP](token: String, failureResult: => RESP)(
      func: TokenAccount => Future[RESP]): Future[RESP] = {
    import system.executionContext
    val ta = SessionUtils.parseAccount(token) match {
      case Right(value)  => value
      case Left(message) => throw new IllegalArgumentException(message)
    }

    userEntity
      .ask[UserResponse](replyTo =>
        ShardingEnvelope(ta.account, UserCommand(replyTo, Cmd.CheckSession(CheckSession(token)))))
      .flatMap {
        case resp if resp.status == IntStatus.OK => func(ta)
        case resp =>
          logger.debug(s"Check session error: $resp")
          Future.successful(failureResult)
      }
      .recover { case _ => failureResult }
  }

  def checkUserSession[RESP](metadata: Metadata, failureResult: => RESP)(
      func: TokenAccount => Future[RESP]): Future[RESP] =
    try {
      val token = metadata
        .getText(Cookie.name)
        .flatMap { value =>
          Cookie.parseFromValueString(value) match {
            case Right(cookie) => SessionUtils.tokenFromCookie(cookie)
            case Left(errors) =>
              logger.debug(s"Parse session from cookie error: $errors.")
              None
          }
        }
        .orElse(metadata.getText(Constants.SESSION_TOKEN_NAME))
        .getOrElse(throw new IllegalArgumentException("Session token missing."))

      checkUserSession(token, failureResult)(func)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Parse session token from header error: ${e.toString}")
        Future.successful(failureResult)
    }
}

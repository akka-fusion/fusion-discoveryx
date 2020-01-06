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

package fusion.discoveryx.server.route

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.grpc.scaladsl.MetadataImpl
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Directive1 }
import akka.util.Timeout
import fusion.discoveryx.server.user.UserEntity
import fusion.discoveryx.server.protocol.{ TokenAccount, UserResponse }
import fusion.discoveryx.server.util.{ CheckUserSessionHelper, SessionUtils }
import helloscala.common.IntStatus

import scala.concurrent.Future

trait SessionRoute {
  def createValidationSession(userEntity: ActorRef[ShardingEnvelope[UserEntity.Command]])(
      implicit system: ActorSystem[_],
      timeout: Timeout): Directive0 =
    createGetSessionUser(userEntity).flatMap {
      case Right(_) =>
        pass
      case Left(_) =>
        import fusion.discoveryx.server.util.ProtobufJsonSupport._
        complete((StatusCodes.Unauthorized, UserResponse(IntStatus.UNAUTHORIZED)))
//        reject(
//          AuthenticationFailedRejection(
//            AuthenticationFailedRejection.CredentialsRejected,
//            HttpChallenges.oAuth2(Constants.SESSION_TOKEN_NAME)))
    }

  def createGetSessionUser(userEntity: ActorRef[ShardingEnvelope[UserEntity.Command]])(
      implicit system: ActorSystem[_],
      timeout: Timeout): Directive1[Either[Int, TokenAccount]] =
    extractRequestContext.flatMap { ctx =>
      val metadata = new MetadataImpl(ctx.request.headers)
      val future =
        new CheckUserSessionHelper(userEntity)
          .checkUserSession[Either[Int, TokenAccount]](metadata, Left(IntStatus.UNAUTHORIZED))(tokenAccount =>
            Future.successful(Right(tokenAccount)))
      onSuccess(future).flatMap { either =>
        provide(either)
      }
    }

  def optionalSessionToken: Directive1[Option[String]] = extractRequest.flatMap { request =>
    provide(SessionUtils.getTokenFromRequest(request))
  }
}

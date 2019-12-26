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
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Directive0 }
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import fusion.discoveryx.server.management.UserEntity
import fusion.discoveryx.server.protocol.UserResponse
import fusion.discoveryx.server.util.CheckUserSessionHelper
import helloscala.common.IntStatus

import scala.concurrent.Future

trait SessionRoute {
  def validationSession(userEntity: ActorRef[ShardingEnvelope[UserEntity.Command]])(
      implicit system: ActorSystem[_],
      timeout: Timeout): Directive0 =
    extractRequestContext.flatMap { ctx =>
      val metadata = new MetadataImpl(ctx.request.headers)
      val future =
        new CheckUserSessionHelper(userEntity).checkUserSession(metadata, UserResponse(IntStatus.UNAUTHORIZED))(_ =>
          Future.successful(UserResponse(IntStatus.OK)))
      onSuccess(future).flatMap {
        case UserResponse(IntStatus.OK, _, _) => pass
        case resp =>
          ctx.log.warning(s"Validation session error: $resp")
          reject(AuthorizationFailedRejection)
      }
    }
}

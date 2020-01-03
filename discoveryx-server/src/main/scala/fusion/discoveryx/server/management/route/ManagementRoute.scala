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

package fusion.discoveryx.server.management.route

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ DateTime, HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Route }
import akka.stream.SystemMaterializer
import akka.util.Timeout
import fusion.core.extension.FusionCore
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.grpc.{ ManagementServiceHandler, UserServiceHandler }
import fusion.discoveryx.server.management.service.{ ManagementServiceImpl, UserServiceImpl }
import fusion.discoveryx.server.management.{ Management, UserEntity, UserManager }
import fusion.discoveryx.server.protocol._
import fusion.discoveryx.server.route.{ SessionRoute, pathPost }
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._

class ManagementRoute()(implicit system: ActorSystem[_]) extends SessionRoute {
  private implicit val timeout: Timeout = 5.seconds
  private val managementRef = Management.init(system)
  private val userEntity = UserEntity.init(system)
  private val userManager = UserManager.init(system)
  private val managementService = new ManagementServiceImpl(managementRef)
  private val userService = new UserServiceImpl(userEntity, userManager)
  private val validationSession: Directive0 = createValidationSession(userEntity)

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    implicit val mat = SystemMaterializer(system).materializer
    implicit val classicSystem = FusionCore(system).classicSystem
    ManagementServiceHandler.partial(managementService) :: UserServiceHandler.partial(userService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def consoleRoute: Route = pathPrefix("management") {
    validationSession {
      pathPost("ListNamespace") {
        entity(as[ListNamespace]) { in =>
          complete(managementService.listNamespace(in))
        }
      } ~
      pathPost("CreateNamespace") {
        entity(as[CreateNamespace]) { in =>
          complete(managementService.createNamespace(in))
        }
      } ~
      pathPost("RemoveNamespace") {
        entity(as[RemoveNamespace]) { in =>
          complete(managementService.removeNamespace(in))
        }
      } ~
      pathPost("ModifyNamespace") {
        entity(as[ModifyNamespace]) { in =>
          complete(managementService.modifyNamespace(in))
        }
      }
    }
  }

  def userRoute: Route = pathPrefix("user") {
    validationSession {
      pathPost("ListUser") {
        entity(as[ListUser]) { in =>
          complete(userService.listUser(in))
        }
      } ~
      pathPost("CreateUser") {
        entity(as[CreateUser]) { in =>
          complete(userService.createUser(in))
        }
      } ~
      pathPost("ModifyUser") {
        entity(as[ModifyUser]) { in =>
          complete(userService.modifyUser(in))
        }
      } ~
      pathPost("RemoveUser") {
        entity(as[RemoveUser]) { in =>
          complete(userService.removeUser(in))
        }
      }
    }
  }

  def signRoute: Route = pathPrefix("sign") {
    (pathEndOrSingleSlash | path("CurrentSessionUser")) {
      (get | post) {
        optionalSessionToken {
          case Some(token) =>
            complete(userService.currentSessionUser(CurrentSessionUser(token)))
          case _ =>
            complete(UserResponse(IntStatus.UNAUTHORIZED, "Token not found."))
        }
      }
    } ~
    pathPost("Login") {
      entity(as[Login]) { in =>
        onSuccess(userService.login(in)) { resp =>
          val cookie = resp.data.logined match {
            case Some(logined) =>
              HttpCookie(
                Constants.SESSION_TOKEN_NAME,
                logined.token,
                Some(DateTime(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli)),
                path = Some("/"))
            case _ => HttpCookie(Constants.SESSION_TOKEN_NAME, "", Some(DateTime.MinValue), path = Some("/"))
          }
          setCookie(cookie) {
            complete(resp)
          }
        }
      }
    } ~
    pathPost("Logout") {
      validationSession {
        entity(as[Logout]) { in =>
          setCookie(HttpCookie(Constants.SESSION_TOKEN_NAME, "", Some(DateTime.MinValue), path = Some("/"))) {
            complete(userService.logout(in))
          }
        }
      }
    }
  }
}

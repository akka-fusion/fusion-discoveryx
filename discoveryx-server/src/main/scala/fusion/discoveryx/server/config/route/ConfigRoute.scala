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

package fusion.discoveryx.server.config.route

import akka.actor
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import akka.util.Timeout
import fusion.core.extension.FusionCore
import fusion.discoveryx.grpc.ConfigServiceHandler
import fusion.discoveryx.model.{ ConfigGet, ConfigItem, ConfigRemove }
import fusion.discoveryx.server.config.service.{ ConfigManagerServiceImpl, ConfigServiceImpl }
import fusion.discoveryx.server.grpc.ConfigManagerServiceHandler
import fusion.discoveryx.server.management.{ NamespaceRef, UserEntity }
import fusion.discoveryx.server.protocol.ListConfig
import fusion.discoveryx.server.route.{ SessionRoute, pathPost }

import scala.concurrent.Future
import scala.concurrent.duration._

class ConfigRoute(namespaceRef: ActorRef[NamespaceRef.ExistNamespace])(implicit system: ActorSystem[_])
    extends SessionRoute {
  private implicit val timeout: Timeout = 5.seconds
  private val configService = new ConfigServiceImpl(namespaceRef)
  private val configManagerService = new ConfigManagerServiceImpl(namespaceRef)
  private val userEntity = UserEntity.init(system)
  private val validationSession = createValidationSession(userEntity)

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = FusionCore(system).classicSystem
    ConfigServiceHandler.partial(configService) :: ConfigManagerServiceHandler.partial(configManagerService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def openRoute: Route = pathPrefix("config") {
    pathPost("GetConfig") {
      entity(as[ConfigGet]) { in =>
        complete(configService.getConfig(in))
      }
    } ~
    pathPost("PublishConfig") {
      validationSession {
        entity(as[ConfigItem]) { in =>
          complete(configService.publishConfig(in))
        }
      }
    } ~
    pathPost("RemoveConfig") {
      validationSession {
        entity(as[ConfigRemove]) { in =>
          complete(configService.removeConfig(in))
        }
      }
    }
  }

  def consoleRoute: Route = pathPrefix("config") {
    validationSession {
      pathPost("ListConfig") {
        entity(as[ListConfig]) { in =>
          complete(configManagerService.listConfig(in))
        }
      }
    }
  }
}

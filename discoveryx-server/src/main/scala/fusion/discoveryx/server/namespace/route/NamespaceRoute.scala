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

package fusion.discoveryx.server.namespace.route

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.SystemMaterializer
import akka.util.Timeout
import fusion.core.extension.FusionCore
import fusion.discoveryx.server.grpc.NamespaceManagerServiceHandler
import fusion.discoveryx.server.namespace.NamespaceManager
import fusion.discoveryx.server.namespace.service.NamespaceManagerServiceImpl
import fusion.discoveryx.server.protocol._
import fusion.discoveryx.server.route.{ SessionRoute, pathPost }
import fusion.discoveryx.server.user.UserEntity

import scala.concurrent.Future
import scala.concurrent.duration._

class NamespaceRoute()(implicit system: ActorSystem[_]) extends SessionRoute {
  private implicit val timeout: Timeout = 5.seconds
  private val namespaceManagerService = new NamespaceManagerServiceImpl(NamespaceManager.init(system))
  private val validationSession = createValidationSession(UserEntity.init(system))

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    implicit val mat = SystemMaterializer(system).materializer
    implicit val classicSystem = FusionCore(system).classicSystem
    NamespaceManagerServiceHandler.partial(namespaceManagerService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def consoleRoute: Route = pathPrefix("management") {
    validationSession {
      pathPost("ListNamespace") {
        entity(as[ListNamespace]) { in =>
          complete(namespaceManagerService.listNamespace(in))
        }
      } ~
      pathPost("CreateNamespace") {
        entity(as[CreateNamespace]) { in =>
          complete(namespaceManagerService.createNamespace(in))
        }
      } ~
      pathPost("RemoveNamespace") {
        entity(as[RemoveNamespace]) { in =>
          complete(namespaceManagerService.removeNamespace(in))
        }
      } ~
      pathPost("ModifyNamespace") {
        entity(as[ModifyNamespace]) { in =>
          complete(namespaceManagerService.modifyNamespace(in))
        }
      }
    }
  }
}

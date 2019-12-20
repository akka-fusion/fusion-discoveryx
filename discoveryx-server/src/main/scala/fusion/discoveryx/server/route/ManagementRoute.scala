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

import akka.actor
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import fusion.discoveryx.server.grpc.ManagementServiceHandler
import fusion.discoveryx.server.management.{ Management, ManagementServiceImpl }
import fusion.discoveryx.server.protocol.{ CreateNamespace, ListNamespace, ModifyNamespace, RemoveNamespace }

import scala.concurrent.Future

class ManagementRoute()(implicit system: ActorSystem[_]) {
  private val managementRef = Management.init(system)
  private val managementService = new ManagementServiceImpl(managementRef)

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    import akka.actor.typed.scaladsl.adapter._
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = system.toClassic
    ManagementServiceHandler.partial(managementService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def consoleRoute: Route = pathPrefix("management") {
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

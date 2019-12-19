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
import akka.grpc.scaladsl.MetadataImpl
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import fusion.common.FusionProtocol
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.grpc.NamingServicePowerApiHandler
import fusion.discoveryx.model.{ InstanceModify, InstanceQuery, InstanceRegister, InstanceRemove }
import fusion.discoveryx.server.grpc.NamingManagerServiceHandler
import fusion.discoveryx.server.naming.service.NamingManagerServiceImpl
import fusion.discoveryx.server.naming.{ NamingServiceImpl, NamingSettings }
import fusion.discoveryx.server.protocol.{ CreateService, GetService, ListService, ModifyService, RemoveService }

import scala.concurrent.Future

class NamingRoute(discoveryX: DiscoveryX, namingSettings: NamingSettings) {
  private implicit val system: ActorSystem[FusionProtocol.Command] = discoveryX.system
  private val namingManagerService = new NamingManagerServiceImpl()(discoveryX.system)
  private val namingService = new NamingServiceImpl()

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = discoveryX.classicSystem
    NamingServicePowerApiHandler.partial(namingService) :: NamingManagerServiceHandler.partial(namingManagerService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def consoleRoute: Route = pathPrefix("naming") {
    pathPost("ListService") {
      entity(as[ListService]) { in =>
        complete(namingManagerService.listService(in))
      }
    } ~
    pathPost("GetService") {
      entity(as[GetService]) { in =>
        complete(namingManagerService.getService(in))
      }
    } ~
    pathPost("CreateService") {
      entity(as[CreateService]) { in =>
        complete(namingManagerService.createService(in))
      }
    } ~
    pathPost("RemoveService") {
      entity(as[RemoveService]) { in =>
        complete(namingManagerService.removeService(in))
      }
    } ~
    pathPost("ModifyService") {
      entity(as[ModifyService]) { in =>
        complete(namingManagerService.modifyService(in))
      }
    }
  }

  def openRoute: Route = pathPrefix("naming") {
    pathPost("QueryInstance") {
      entity(as[InstanceQuery]) { in =>
        complete(namingService.queryInstance(in, new MetadataImpl()))
      }
    } ~
    pathPost("RegisterInstance") {
      entity(as[InstanceRegister]) { in =>
        complete(namingService.registerInstance(in, new MetadataImpl()))
      }
    } ~
    pathPost("ModifyInstance") {
      entity(as[InstanceModify]) { in =>
        complete(namingService.modifyInstance(in, new MetadataImpl()))
      }
    } ~
    pathPost("RemoveInstance") {
      entity(as[InstanceRemove]) { in =>
        complete(namingService.removeInstance(in, new MetadataImpl()))
      }
    }
  }
}

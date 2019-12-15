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
import fusion.discoveryx.server.protocol.{ GetService, ListService }

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

  def managementRoute: Route = pathPrefix("naming") {
    pathPost("listService") {
      entity(as[ListService]) { cmd =>
        complete(namingManagerService.listService(cmd))
      }
    } ~
    pathPost("getService") {
      entity(as[GetService]) { cmd =>
        complete(namingManagerService.getService(cmd))
      }
    } ~
    pathPost("createInstance") {
      entity(as[InstanceRegister]) { cmd =>
        complete(namingManagerService.createInstance(cmd))
      }
    } ~
    pathPost("removeInstance") {
      entity(as[InstanceRemove]) { cmd =>
        complete(namingManagerService.removeInstance(cmd))
      }
    } ~
    pathPost("modifyInstance") {
      entity(as[InstanceModify]) { cmd =>
        complete(namingManagerService.modifyInstance(cmd))
      }
    }
  }

  def openRoute: Route = pathPrefix("naming") {
    pathPost("queryInstance") {
      entity(as[InstanceQuery]) { cmd =>
        complete(namingService.queryInstance(cmd, new MetadataImpl()))
      }
    } ~
    pathPost("registerInstance") {
      entity(as[InstanceRegister]) { cmd =>
        complete(namingService.registerInstance(cmd, new MetadataImpl()))
      }
    } ~
    pathPost("modifyInstance") {
      entity(as[InstanceModify]) { cmd =>
        complete(namingService.modifyInstance(cmd, new MetadataImpl()))
      }
    } ~
    pathPost("removeInstance") {
      entity(as[InstanceRemove]) { cmd =>
        complete(namingService.removeInstance(cmd, new MetadataImpl()))
      }
    }
  }
}

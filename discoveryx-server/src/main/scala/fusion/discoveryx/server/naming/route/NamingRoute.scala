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

package fusion.discoveryx.server.naming.route

import akka.actor
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.grpc.scaladsl.MetadataImpl
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import akka.util.Timeout
import fusion.core.extension.FusionCore
import fusion.discoveryx.grpc.NamingServicePowerApiHandler
import fusion.discoveryx.model.{ InstanceModify, InstanceQuery, InstanceRegister, InstanceRemove }
import fusion.discoveryx.server.grpc.NamingManagerServiceHandler
import fusion.discoveryx.server.namespace.NamespaceRef.ExistNamespace
import fusion.discoveryx.server.user.UserEntity
import fusion.discoveryx.server.naming.service.{ NamingManagerServiceImpl, NamingServiceImpl }
import fusion.discoveryx.server.protocol._
import fusion.discoveryx.server.route.{ SessionRoute, pathPost }

import scala.concurrent.Future
import scala.concurrent.duration._

class NamingRoute(namespaceRef: ActorRef[ExistNamespace])(implicit system: ActorSystem[_]) extends SessionRoute {
  private implicit val timeout: Timeout = 5.seconds
  private val namingManagerService = new NamingManagerServiceImpl(namespaceRef)
  private val namingService = new NamingServiceImpl(namespaceRef)
  private val userEntity = UserEntity.init(system)
  private val validationSession = createValidationSession(userEntity)

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = FusionCore(system).classicSystem
    NamingServicePowerApiHandler.partial(namingService) :: NamingManagerServiceHandler.partial(namingManagerService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def consoleRoute: Route = pathPrefix("naming") {
    validationSession {
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
      } ~
      modifyInstanceRoute ~
      removeInstanceRoute
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
    modifyInstanceRoute ~
    removeInstanceRoute
  }

  private def modifyInstanceRoute: Route = pathPost("ModifyInstance") {
    entity(as[InstanceModify]) { in =>
      complete(namingService.modifyInstance(in, new MetadataImpl()))
    }
  }

  private def removeInstanceRoute: Route = pathPost("RemoveInstance") {
    entity(as[InstanceRemove]) { in =>
      complete(namingService.removeInstance(in, new MetadataImpl()))
    }
  }
}

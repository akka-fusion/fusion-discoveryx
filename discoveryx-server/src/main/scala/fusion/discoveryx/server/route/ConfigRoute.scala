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
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import fusion.discoveryx.grpc.ConfigServiceHandler
import fusion.discoveryx.model.{ ConfigGet, ConfigItem, ConfigRemove }
import fusion.discoveryx.server.config.{ ConfigManagerServiceImpl, ConfigServiceImpl }
import fusion.discoveryx.server.grpc.ConfigManagerServiceHandler
import fusion.discoveryx.server.management.NamespaceRef
import fusion.discoveryx.server.protocol.ListConfig

import scala.concurrent.Future

class ConfigRoute(namespaceRef: ActorRef[NamespaceRef.ExistNamespace])(implicit system: ActorSystem[_]) {
  private val configService = new ConfigServiceImpl(namespaceRef)
  private val configManagerService = new ConfigManagerServiceImpl(namespaceRef)

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    import akka.actor.typed.scaladsl.adapter._
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = system.toClassic
    ConfigServiceHandler.partial(configService) :: ConfigManagerServiceHandler.partial(configManagerService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def openRoute: Route = pathPrefix("config") {
    pathPost("QueryConfig") {
      entity(as[ConfigGet]) { in =>
        complete(configService.queryConfig(in))
      }
    } ~
    pathPost("PublishConfig") {
      entity(as[ConfigItem]) { in =>
        complete(configService.publishConfig(in))
      }
    } ~
    pathPost("RemoveConfig") {
      entity(as[ConfigRemove]) { in =>
        complete(configService.removeConfig(in))
      }
    }
  }

  def consoleRoute: Route = pathPrefix("config") {
    pathPost("ListConfig") {
      entity(as[ListConfig]) { in =>
        complete(configManagerService.listConfig(in))
      }
    } ~
    pathPost("GetConfig") {
      entity(as[ConfigGet]) { in =>
        complete(configManagerService.getConfig(in))
      }
    } ~
    pathPost("PublishConfig") {
      entity(as[ConfigItem]) { in =>
        complete(configManagerService.publishConfig(in))
      }
    } ~
    pathPost("RemoveConfig") {
      entity(as[ConfigRemove]) { in =>
        complete(configManagerService.removeConfig(in))
      }
    }
  }
}

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
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import fusion.common.FusionProtocol
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.grpc.ConfigServiceHandler
import fusion.discoveryx.model.{ ConfigGet, ConfigItem, ConfigRemove }
import fusion.discoveryx.server.ConfigLeader
import fusion.discoveryx.server.config.{ ConfigManagerServiceImpl, ConfigServiceImpl, ConfigSettings }
import fusion.discoveryx.server.grpc.ConfigManagerServiceHandler
import fusion.discoveryx.server.protocol.ListConfig

import scala.concurrent.Future

class ConfigRoute(discoveryX: DiscoveryX, configSettings: ConfigSettings) {
  implicit val system: ActorSystem[FusionProtocol.Command] = discoveryX.system
  ClusterSingleton(system).init(SingletonActor(ConfigLeader(), ConfigLeader.NAME))
  private val configService = new ConfigServiceImpl()
  private val configManagerService = new ConfigManagerServiceImpl()

  val grpcHandler: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = {
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = discoveryX.classicSystem
    ConfigServiceHandler.partial(configService) :: ConfigManagerServiceHandler.partial(configManagerService) :: Nil
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def openRoute: Route = pathPrefix("config") {
    pathPost("queryConfig") {
      entity(as[ConfigGet]) { in =>
        complete(configService.queryConfig(in))
      }
    } ~
    pathPost("publishConfig") {
      entity(as[ConfigItem]) { in =>
        complete(configService.publishConfig(in))
      }
    } ~
    pathPost("removeConfig") {
      entity(as[ConfigRemove]) { in =>
        complete(configService.removeConfig(in))
      }
    }
  }

  def managementRoute: Route = pathPrefix("config") {
    pathPost("listConfig") {
      entity(as[ListConfig]) { in =>
        complete(configManagerService.listConfig(in))
      }
    } ~
    pathPost("getConfig") {
      entity(as[ConfigGet]) { in =>
        complete(configManagerService.getConfig(in))
      }
    } ~
    pathPost("publishConfig") {
      entity(as[ConfigItem]) { in =>
        complete(configManagerService.publishConfig(in))
      }
    } ~
    pathPost("removeConfig") {
      entity(as[ConfigRemove]) { in =>
        complete(configManagerService.removeConfig(in))
      }
    }
  }
}

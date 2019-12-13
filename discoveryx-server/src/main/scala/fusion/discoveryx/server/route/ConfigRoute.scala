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
import akka.actor.typed.{ ActorRef, ActorSystem, SupervisorStrategy }
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, SystemMaterializer }
import fusion.common.FusionProtocol
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.grpc.ConfigServiceHandler
import fusion.discoveryx.server.config.{ ConfigManager, ConfigServiceImpl, ConfigSettings }

import scala.concurrent.Future
import scala.concurrent.duration._

class ConfigRoute(discoveryX: DiscoveryX, configSettings: ConfigSettings) {
  implicit val system: ActorSystem[FusionProtocol.Command] = discoveryX.system

  def openRoute: Route = pathPrefix("config") {
    complete(StatusCodes.NotImplemented)
  }
  def managementRoute: Route = pathPrefix("config") {
    complete(StatusCodes.NotImplemented)
  }

  val grpcHandler: PartialFunction[HttpRequest, Future[HttpResponse]] = {
    implicit val mat: Materializer = SystemMaterializer(system).materializer
    implicit val classicSystem: actor.ActorSystem = discoveryX.classicSystem
    val configManager: ActorRef[ConfigManager.Command] = discoveryX.spawnActorSync(
      Behaviors.supervise(ConfigManager()).onFailure(SupervisorStrategy.restart),
      ConfigManager.NAME,
      2.seconds)
    ConfigServiceHandler.partial(new ConfigServiceImpl(configManager))
  }
}

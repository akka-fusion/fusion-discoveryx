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

import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.config.ConfigSettings
import fusion.discoveryx.server.naming.NamingSettings

import scala.concurrent.Future

class Routes(discoveryX: DiscoveryX, configSettings: ConfigSettings, namingSettings: NamingSettings)
    extends StrictLogging {
  private var openRoutes: List[Route] = Nil
  private var managementRoutes: List[Route] = Nil
  private var grpcHandlers: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = Nil
  if (configSettings.enable) {
    val c = new ConfigRoute(discoveryX, configSettings)
    openRoutes ::= c.openRoute
    managementRoutes ::= c.managementRoute
    grpcHandlers :::= c.grpcHandler
  }
  if (namingSettings.enable) {
    val n = new NamingRoute(discoveryX, namingSettings)
    openRoutes ::= n.openRoute
    managementRoutes ::= n.managementRoute
    grpcHandlers :::= n.grpcHandler
  }
  private val grpcHandler = ServiceHandler.concatOrNotFound(grpcHandlers: _*)

  val route: Route = {
    pathPrefix("fusion" / Constants.DISCOVERYX) {
      pathPrefix("v1") {
        concat(openRoutes: _*)
      } ~
      pathPrefix("management") {
        concat(managementRoutes: _*)
      }
    } ~
    extractRequest { request =>
      onSuccess(grpcHandler(request)) { response =>
        complete(response)
      }
    }
  }
}

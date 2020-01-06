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

import akka.cluster.typed.Cluster
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.DiscoveryX
import fusion.discoveryx.server.config.route.ConfigRoute
import fusion.discoveryx.server.namespace.route.NamespaceRoute
import fusion.discoveryx.server.user.route.UserRoute
import fusion.discoveryx.server.naming.route.NamingRoute

import scala.concurrent.Future

class Routes(discoveryX: DiscoveryX) extends StrictLogging {
  private implicit val system = discoveryX.system
  private val roles = Cluster(system).selfMember.roles
  private var openRoutes: List[Route] = Nil
  private var consoleRoutes: List[Route] = Nil
  private var grpcHandlers: List[PartialFunction[HttpRequest, Future[HttpResponse]]] = Nil

  def init(): Routes = {
    logger.debug(s"Cluster roles: $roles.")
    if (roles(Constants.MANAGEMENT)) {
      val u = new UserRoute()
      val n = new NamespaceRoute()
      consoleRoutes ::= u.consoleRoute
      consoleRoutes ::= u.signRoute
      consoleRoutes ::= n.consoleRoute
      grpcHandlers :::= u.grpcHandler
      grpcHandlers :::= n.grpcHandler
    }
    if (roles(Constants.CONFIG)) {
      val c = new ConfigRoute(discoveryX.namespaceRef)
      openRoutes ::= c.openRoute
      consoleRoutes ::= c.consoleRoute
      grpcHandlers :::= c.grpcHandler
    }
    if (roles(Constants.NAMING)) {
      val n = new NamingRoute(discoveryX.namespaceRef)
      openRoutes ::= n.openRoute
      consoleRoutes ::= n.consoleRoute
      grpcHandlers :::= n.grpcHandler
    }
    this
  }

  def route: Route = {
    val grpcHandler: HttpRequest => Future[HttpResponse] = ServiceHandler.concatOrNotFound(grpcHandlers: _*)
    var routes = List(extractRequest { request =>
      onSuccess(grpcHandler(request)) { response =>
        complete(response)
      }
    })
    if (roles(Constants.MANAGEMENT)) {
      routes ::= get {
        getFromResourceDirectory("dist") ~
        getFromResource("dist/index.html")
      }
    }
    routes ::= pathPrefix("fusion" / Constants.DISCOVERYX) {
      pathPrefix("v1") {
        concat(openRoutes: _*)
      } ~
      pathPrefix("console") {
        concat(consoleRoutes: _*)
      }
    }

    concat(routes: _*)
  }
}

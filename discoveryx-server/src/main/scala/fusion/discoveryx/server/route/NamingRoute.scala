/*
 * Copyright 2019 helloscala.com
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

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.server.naming.{ NamingManagerService, NamingSettings }
import fusion.discoveryx.server.protocol.{ GetService, ListService }

class NamingRoute(discoveryX: DiscoveryX, namingSettings: NamingSettings) {
  private val namingManagerService = new NamingManagerService()(discoveryX.system)

  def route: Route = pathPrefix("naming") {
    listServiceRoute ~
    getServiceRoute
  }

  import fusion.discoveryx.server.util.ProtobufJsonSupport._

  def listServiceRoute: Route = pathPost("list") {
    entity(as[ListService]) { cmd =>
      complete(namingManagerService.listService(cmd))
    }
  }

  def getServiceRoute: Route = pathPost("get") {
    entity(as[GetService]) { cmd =>
      complete(namingManagerService.getService(cmd))
    }
  }
}

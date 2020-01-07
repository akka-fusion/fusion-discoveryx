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

package fusion.discoveryx.server.user.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.user.{ UserEntity, UserManager }
import fusion.discoveryx.server.user.service.UserServiceImpl
import fusion.discoveryx.server.protocol.{ CreateNamespace, ListNamespace, Login, ManagementResponse }
import fusion.discoveryx.server.route.FusionRouteTest
import fusion.discoveryx.server.util.ProtobufJson4s
import org.scalatest.WordSpec

class NamespaceManagementRouteTest extends WordSpec with FusionRouteTest with StrictLogging {
  private var sessionToken = ""
  private val route = new UserRoute()(discoveryX.system)

  "Namspace" must {
    import fusion.discoveryx.server.util.ProtobufJsonSupport._

    "CreateNamespace" in {
      Post("/management/CreateNamespace", CreateNamespace("public", "默认名称空间"))
        .withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> route.consoleRoute ~> check {
        println(s"CreateNamespace response: $response")
        status should be(StatusCodes.OK)
        val resp = responseAs[ManagementResponse]
        println(ProtobufJson4s.toJsonPrettyString(resp))
      }
    }

    "ListNamespace" in {
      Post("/management/ListNamespace", ListNamespace())
        .withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> route.consoleRoute ~> check {
        println(s"ListNamespace response: $response")
        status should be(StatusCodes.OK)
        val resp = responseAs[ManagementResponse]
        println(ProtobufJson4s.toJsonPrettyString(resp))
      }
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    initSessionToken()
  }

  def initSessionToken(): String = {
    implicit val st = discoveryX.system
    val userService = new UserServiceImpl(UserEntity.init(st), UserManager.init(st))
    val response = userService.login(Login("discoveryx", "discoveryx")).futureValue
    val logined = response.data.logined.value
    sessionToken = logined.token
    println(s"session token: $logined")
    sessionToken
  }
}

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

package fusion.discoveryx.server.management.route

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{ HttpRequest, StatusCodes }
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.protocol.{ ListUser, Login, UserResponse }
import fusion.discoveryx.server.route.FusionRouteTest
import fusion.discoveryx.server.util.ProtobufJson4s
import org.scalatest.WordSpec

class UserManagementRouteTest extends WordSpec with FusionRouteTest {
  private val managementRoute = new ManagementRoute()(discoveryX.system)

  "UserRoute" must {
    import fusion.discoveryx.server.util.ProtobufJsonSupport._
    val account = Constants.DISCOVERYX
    val password = Constants.DISCOVERYX
    var token = ""

    def sendPost[T](uri: String, payload: T)(implicit m: ToEntityMarshaller[T]): HttpRequest = {
      Post(uri, payload).withHeaders(Cookie(Constants.SESSION_TOKEN_NAME, token))
    }

    "Login" in {
      val in = Login(account, password)
      Post("/sign/Login", in) ~> managementRoute.signRoute ~> check {
        status should be(StatusCodes.OK)
        val resp = responseAs[UserResponse]
        println(ProtobufJson4s.toJsonPrettyString(resp))
        val logined = resp.data.logined.value
        token = logined.token
      }
    }

    "ListUser" in {
      val in = ListUser()
      sendPost("/user/ListUser", in) ~> managementRoute.userRoute ~> check {
        println(response)
        status should be(StatusCodes.OK)
      }
    }

//    "CreateUser" in {
//      val in = CreateUser(account, password)
//      Post("/user/create", in) ~> managementRoute.userRoute ~> check {
//        status should be(StatusCodes.OK)
//        val resp = responseAs[UserResponse]
//        println(ProtobufJson4s.toJsonPrettyString(resp))
//        resp.data.user should not be empty
//      }
//    }
  }
}

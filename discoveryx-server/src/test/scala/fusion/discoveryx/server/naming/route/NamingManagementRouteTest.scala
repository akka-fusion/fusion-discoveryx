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

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model.{ Instance, InstanceModify, InstanceRegister, InstanceRemove }
import fusion.discoveryx.server.management.{ Management, UserEntity, UserManager }
import fusion.discoveryx.server.management.service.UserServiceImpl
import fusion.discoveryx.server.protocol.{ GetService, ListService, Login, NamingResponse }
import fusion.discoveryx.server.route.FusionRouteTest
import fusion.discoveryx.server.util.ProtobufJson4s
import helloscala.common.IntStatus
import org.scalatest.WordSpec

class NamingManagementRouteTest extends WordSpec with FusionRouteTest with StrictLogging {
  private var sessionToken = ""
  lazy private val namingRoute = new NamingRoute(discoveryX.namespaceRef)(discoveryX.system)

  "testManagementRoute" must {
    import fusion.discoveryx.server.util.ProtobufJsonSupport._
    val namespace = "827919ee-2d41-11ea-b29f-feff9c9c1fac"
    val serviceName = "fusion-discoveryx"
    val groupName = "default"
    var inst: Instance = null

    "RegisterInstance" in {
      val in = InstanceRegister(namespace, serviceName, groupName, "127.0.0.1", 8000, enable = true, health = true)
      Post("/naming/RegisterInstance", in).withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> namingRoute.openRoute ~> check {
        println("response: " + response)
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"RegisterInstance namingResponse is $namingResponse")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        inst = namingResponse.data.instance.value
//        inst.serviceName should be(serviceName)
        inst.healthy should be(true)
        inst.enabled should be(true)
      }
    }

    "ListService" in {
      val in = ListService(namespace, 1, 20)
      Post("/naming/ListService", in).withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> namingRoute.consoleRoute ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"ListService namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        val listedService = namingResponse.data.listedService.value
        listedService.serviceInfos should not be empty
      }
    }

    "ModifyInstance" in {
      inst should not be null
      val in = InstanceModify(namespace, serviceName, inst.instanceId, enable = Some(false))
      Post("/naming/ModifyInstance", in).withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> namingRoute.openRoute ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"ModifyInstance namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        inst = namingResponse.data.instance.value
      }
    }

    "GetService" in {
      Post("/naming/GetService", GetService(namespace, serviceName))
        .withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> namingRoute.consoleRoute ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"GetService namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        val serviceInfo = namingResponse.data.serviceInfo.value
        serviceInfo.instances should not be empty
        inst should not be null
        serviceInfo.instances should contain(inst)
      }
    }

    "RemoveInstance" in {
      inst should not be null
      Post("/naming/RemoveInstance", InstanceRemove(namespace, serviceName, inst.instanceId))
        .withHeaders(RawHeader(Constants.SESSION_TOKEN_NAME, sessionToken)) ~> namingRoute.openRoute ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"RemoveInstance namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
      }
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Management.init(discoveryX.system)
    initSessionToken()
  }

  def initSessionToken(): String = {
    implicit val st = discoveryX.system
    val userService = new UserServiceImpl(UserEntity.init(st), UserManager.init(st))
    val response = userService.login(Login("discoveryx", "discoveryx")).futureValue
    val logined = response.data.logined.value
    sessionToken = logined.token
    sessionToken
  }
}

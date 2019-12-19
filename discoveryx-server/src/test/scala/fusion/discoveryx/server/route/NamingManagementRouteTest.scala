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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import com.typesafe.config.{ Config, ConfigFactory }
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.model.{ Instance, InstanceModify, InstanceRegister, InstanceRemove }
import fusion.discoveryx.server.naming.NamingSettings
import fusion.discoveryx.server.protocol.{ GetService, ListService, NamingResponse }
import helloscala.common.IntStatus
import org.scalatest.{ Matchers, OptionValues, WordSpec }

import scala.concurrent.duration._
import akka.testkit.TestDuration
import com.typesafe.scalalogging.StrictLogging
import fusion.core.extension.FusionCore
import fusion.discoveryx.server.util.ProtobufJson4s

class NamingManagementRouteTest
    extends WordSpec
    with ScalatestRouteTest
    with Matchers
    with OptionValues
    with StrictLogging {
  private implicit val timeout: RouteTestTimeout = RouteTestTimeout(5.seconds.dilated)
  private var discoveryX: DiscoveryX = _
  lazy private val namingRoute = new NamingRoute(discoveryX, NamingSettings(discoveryX.system))
  lazy private val route = namingRoute.consoleRoute

  "testManagementRoute" must {
    import fusion.discoveryx.server.util.ProtobufJsonSupport._
    val namespace = "namespace"
    val serviceName = "fusion-discoveryx"
    val groupName = "default"
    var inst: Instance = null

    "createInstance" in {
      val in = InstanceRegister(namespace, serviceName, groupName, "127.0.0.1", 8000, enable = true)
      Post("/naming/createInstance", in) ~> route ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"createInstance namingResponse is $namingResponse")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        inst = namingResponse.data.instance.value
        inst.serviceName should be(serviceName)
        inst.healthy should be(true)
        inst.enabled should be(true)
      }
    }

    "listService" in {
      val in = ListService(namespace, 1, 20)
      Post("/naming/listService", in) ~> route ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"listService namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        val listedService = namingResponse.data.listedService.value
        listedService.serviceInfos should not be empty
      }
    }

    "modifyInstance" in {
      inst should not be null
      val in = InstanceModify(namespace, serviceName, inst.instanceId, enable = Some(false))
      Post("/naming/modifyInstance", in) ~> route ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"listService namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        inst = namingResponse.data.instance.value
      }
    }

    "getService" in {
      inst should not be null
      Post("/naming/getService", GetService(namespace, serviceName)) ~> route ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"listService namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
        val serviceInfo = namingResponse.data.serviceInfo.value
        serviceInfo.instances should not be empty
        serviceInfo.instances should contain(inst)
      }
    }

    "removeInstance" in {
      inst should not be null
      Post("/naming/removeInstance", InstanceRemove(namespace, serviceName, inst.instanceId)) ~> route ~> check {
        status should be(StatusCodes.OK)
        val namingResponse = responseAs[NamingResponse]
        logger.debug(s"listService namingResponse is ${ProtobufJson4s.toJsonString(namingResponse)}")
        IntStatus.isSuccess(namingResponse.status) shouldBe true
      }
    }
  }

  override protected def createActorSystem(): ActorSystem = {
    discoveryX = DiscoveryX.fromOriginalConfig(ConfigFactory.load("application-test.conf"))
    FusionCore(discoveryX.system)
    discoveryX.classicSystem
  }

  override def testConfig: Config = discoveryX.config
}

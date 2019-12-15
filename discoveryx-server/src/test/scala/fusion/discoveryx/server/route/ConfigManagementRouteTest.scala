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

import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.model.{ ConfigGet, ConfigItem, ConfigRemove }
import fusion.discoveryx.server.config.ConfigSettings
import fusion.discoveryx.server.protocol.{ ConfigResponse, ListConfig }
import fusion.discoveryx.server.util.ProtobufJson4s
import helloscala.common.IntStatus
import org.scalatest.WordSpec

class ConfigManagementRouteTest extends WordSpec with FusionRouteTest with StrictLogging {
  lazy private val configRoute = new ConfigRoute(discoveryX, ConfigSettings(discoveryX.system))
  lazy private val route = configRoute.managementRoute

  "ConfigManagementRoute" must {
    import fusion.discoveryx.server.util.ProtobufJsonSupport._
    val namespace = "me.yangbajing"
    val dataId = "akka"
    val content =
      """discoveryx {
        |  server {
        |    enable = true
        |  }
        |}""".stripMargin
    var configItem: ConfigItem = null

    "publishConfig" in {
      val in = ConfigItem(namespace, dataId, content = content)
      Post("/config/publishConfig", in) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val configResponse = responseAs[ConfigResponse]
        println("publishConfig response is " + ProtobufJson4s.toJsonString(configResponse))
        IntStatus.isSuccess(configResponse.status) shouldBe true
        configItem = configResponse.data.config.value
        configItem.content shouldBe content
      }
    }

    "listConfig" in {
      val in = ListConfig(namespace)
      Post("/config/listConfig", in) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val configResponse = responseAs[ConfigResponse]
        println("listConfig response is " + ProtobufJson4s.toJsonString(configResponse))
        IntStatus.isSuccess(configResponse.status) shouldBe true
        val listed = configResponse.data.listed.value
        listed.configs should have size 1
      }
    }

    "getConfig" in {
      val in = ConfigGet(namespace, dataId)
      Post("/config/getConfig", in) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val configResponse = responseAs[ConfigResponse]
        println("getConfig response is " + ProtobufJson4s.toJsonString(configResponse))
        IntStatus.isSuccess(configResponse.status) shouldBe true
        val config = configResponse.data.config.value
        config.content shouldBe configItem.content
      }
    }

    "removeConfig" in {
      Post("/config/removeConfig", ConfigRemove(namespace, dataId)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val configResponse = responseAs[ConfigResponse]
        println("getConfig response is " + ProtobufJson4s.toJsonString(configResponse))
        IntStatus.isSuccess(configResponse.status) shouldBe true
      }

      Post("/config/getConfig", ConfigGet(namespace, dataId)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val configResponse = responseAs[ConfigResponse]
        println("getConfig response is " + ProtobufJson4s.toJsonString(configResponse))
        configResponse.status should be(IntStatus.OK)
      }
    }
  }
}

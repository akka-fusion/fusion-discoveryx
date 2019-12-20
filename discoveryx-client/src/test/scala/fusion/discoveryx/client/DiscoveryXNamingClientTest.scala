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

package fusion.discoveryx.client

import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import fusion.discoveryx.model.{ Instance, InstanceQuery, InstanceRegister, InstanceRemove, ServerStatusQuery }
import helloscala.common.IntStatus
import org.scalatest.{ OptionValues, WordSpecLike }

class DiscoveryXNamingClientTest extends ScalaTestWithActorTestKit with WordSpecLike with OptionValues {
  private val settings = NamingClientSettings(system)
  private val namingClient = DiscoveryXNamingClient(settings, system)
  private val namespace = settings.namespace.getOrElse("890cd0cd-22d8-11ea-8bfe-5254002e9e52")
  private val serviceName = "akka"
  private val groupName = ""

  "DiscoveryXNamingService" must {
    var inst: Instance = null
    "serverStatus" in {
      val result = namingClient.serverStatus(ServerStatusQuery()).futureValue
      result.status should be(IntStatus.OK)
    }
    "registerInstance" in {
      val reply = namingClient.registerOnSettings().futureValue
      println(reply)
      reply.status should be(IntStatus.OK)
    }
    "registerInstance2" in {
      val in =
        InstanceRegister(namespace, serviceName, groupName, "127.0.0.1", 8002, enable = true)
      val reply = namingClient.registerInstance(in).futureValue
      println(reply)
      reply.status should be(IntStatus.OK)
      inst = reply.data.instance.value
    }
    "queryInstance all healthy" in {
      val in = InstanceQuery(namespace, serviceName, groupName, allHealthy = true)
      val reply = namingClient.queryInstance(in).futureValue
      reply.status should be(IntStatus.OK)
      val queried = reply.data.serviceInfo.value
      queried.instances should have size 2
    }
    "queryInstance one healthy" in {
      val in = InstanceQuery(namespace, serviceName, groupName, oneHealthy = true)
      val reply = namingClient.queryInstance(in).futureValue
      reply.status should be(IntStatus.OK)
      val queried = reply.data.serviceInfo.value
      queried.instances should have size 1
    }
    "removeInstance" in {
      inst should not be null
      val reply =
        namingClient.removeInstance(InstanceRemove(namespace, serviceName, inst.instanceId)).futureValue
      reply.status should be(IntStatus.OK)
    }
    "queryInstance all healthy again" in {
      val in = InstanceQuery(namespace, serviceName, groupName, allHealthy = true)
      val reply = namingClient.queryInstance(in).futureValue
      reply.status should be(IntStatus.OK)
      val queried = reply.data.serviceInfo.value
      queried.instances should have size 1
      queried.instances.exists(_.namespace == namespace) shouldBe true
      queried.instances.exists(_.serviceName == serviceName) shouldBe true
      queried.instances.exists(_.groupName == groupName) shouldBe true
    }
    "hearbeat" in {
      TimeUnit.SECONDS.sleep(10)
    }
  }
}

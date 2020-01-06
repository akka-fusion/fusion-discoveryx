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

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.typesafe.config.ConfigFactory
import fusion.discoveryx.model.InstanceQuery
import org.scalatest.WordSpecLike

class DefaultNamingClientTest
    extends ScalaTestWithActorTestKit(ConfigFactory.load("application-local.conf"))
    with WordSpecLike {
  private implicit val ec = system.executionContext
  private val client = DefaultNamingClient(system)
  "DefaultNamingClient" must {
    "url" in {
      val url = "http://fusion-schedulerx/cluster/health"
//      val uris = Future.sequence(Vector.fill(4)(client.generateUri(url))).futureValue
      val uris = (0 until 4).map(_ => client.generateUri(url).futureValue).toVector
      println(uris)
    }

    "queryService" in {
      val reply = client.queryInstance(InstanceQuery(client.settings.namespace.get, "fusion-schedulerx")).futureValue
      println(reply.status)
      reply.getServiceInfo.instances.foreach(println)
    }
  }
}

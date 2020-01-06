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

package fusion.discoveryx.client.play.scaladsl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import play.api.libs.ws.ahc.AhcWSClient

class DiscoveryXWSClientTest extends ScalaTestWithActorTestKit with WordSpecLike {
  "StandaloneWSClient" must {
    "create" in {
      // #standaloneWSClient-create
      val client = DiscoveryXWSClient.standaloneWSClient()
      val url = "http://fusion-schedulerx/cluster/health"
      val req = client.url(url)
      req.url should not be url
      req.url should not contain "fusion-schedulerx"
      // #standaloneWSClient-create
    }
  }

  "WSClient" must {
    "create" in {
      val client = DiscoveryXWSClient.wsClient()
      val url = "http://fusion-schedulerx/cluster/health"
      val req = client.url(url)
      req.url should not be url
      req.url should not contain "fusion-schedulerx"
    }

    "wrapper" in {
      val client = DiscoveryXWSClient.wsClient(AhcWSClient())
      val url = "http://fusion-schedulerx/cluster/health"
      val req = client.url(url)
      req.url should not be url
      req.url should not contain "fusion-schedulerx"
    }
  }
}

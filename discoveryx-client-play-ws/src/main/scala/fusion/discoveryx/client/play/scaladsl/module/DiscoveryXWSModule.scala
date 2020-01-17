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

package fusion.discoveryx.client.play.scaladsl.module

import akka.actor.typed.ActorSystem
import fusion.discoveryx.client.play.scaladsl.{ DiscoveryXPlay, DiscoveryXPlayWSClient, DiscoveryXWSClient }
import fusion.discoveryx.common.Constants
import javax.inject.{ Inject, Provider, Singleton }
import play.api.inject.{ SimpleModule, bind }
import play.api.libs.ws.WSClient
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

class DiscoveryXWSModule
    extends SimpleModule(
      bind[DiscoveryXPlayWSClient].toProvider[DiscoveryXPlayWSClientProvider],
      bind[WSClient].qualifiedWith(Constants.DISCOVERYX).to[DiscoveryXPlayWSClient],
      bind[WSClient].qualifiedWith[DiscoveryXPlay].to[DiscoveryXPlayWSClient])

@Singleton
class DiscoveryXPlayWSClientProvider @Inject() (asyncHttpClient: AsyncHttpClient)(implicit system: ActorSystem[_])
    extends Provider[DiscoveryXPlayWSClient] {
  override lazy val get: DiscoveryXPlayWSClient = DiscoveryXWSClient.wsClient(asyncHttpClient)
}

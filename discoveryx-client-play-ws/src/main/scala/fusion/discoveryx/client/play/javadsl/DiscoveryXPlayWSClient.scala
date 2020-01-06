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

package fusion.discoveryx.client.play.javadsl

import akka.actor.typed.ActorSystem
import fusion.discoveryx.client.{ DefaultNamingClient, NamingClient }
import play.libs.ws.{ WSClient, WSRequest }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

final class DiscoveryXPlayWSClient(client: WSClient)(implicit system: ActorSystem[_]) extends WSClient {
  val namingClient: NamingClient = DefaultNamingClient(system)

  override def getUnderlying: AnyRef = client.getUnderlying

  override def asScala(): play.api.libs.ws.WSClient = client.asScala()

  override def url(uri: String): WSRequest = url(uri, namingClient.settings.queryTimeout)

  def url(uri: String, timeout: FiniteDuration): WSRequest = Await.result(asyncUrl(uri), timeout)

  def asyncUrl(url: String): Future[WSRequest] =
    namingClient.generateUri(url).map(uri => client.url(uri.map(_.toString()).getOrElse(url)))(system.executionContext)

  override def close(): Unit = client.close()
}

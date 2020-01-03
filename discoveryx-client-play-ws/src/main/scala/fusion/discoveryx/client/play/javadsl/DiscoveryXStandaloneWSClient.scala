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
import play.libs.ws.{ StandaloneWSClient, StandaloneWSRequest }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

final class DiscoveryXStandaloneWSClient(client: StandaloneWSClient)(implicit system: ActorSystem[_])
    extends StandaloneWSClient {
  import system.executionContext
  val namingClient: NamingClient = DefaultNamingClient(system)

  override def getUnderlying: AnyRef = client.getUnderlying

  override def url(uri: String): StandaloneWSRequest = url(uri, 5.seconds)

  def url(uri: String, timeout: FiniteDuration): StandaloneWSRequest = Await.result(asyncUrl(uri), timeout)

  def asyncUrl(url: String): Future[StandaloneWSRequest] =
    namingClient.generateUri(url).map(uri => client.url(uri.toString()))

  override def close(): Unit = client.close()
}

object DiscoveryXStandaloneWSClient {
  // Java API
  def create(client: StandaloneWSClient, system: ActorSystem[_]): StandaloneWSClient =
    new DiscoveryXStandaloneWSClient(client)(system)
}

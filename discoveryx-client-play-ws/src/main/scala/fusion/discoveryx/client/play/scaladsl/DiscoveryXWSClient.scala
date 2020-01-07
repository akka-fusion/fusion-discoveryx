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

import akka.actor.typed.ActorSystem
import fusion.discoveryx.client.{ DefaultNamingClient, NamingClient }
import play.api.libs.ws.ahc.{ AhcWSClient, StandaloneAhcWSClient }
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSRequest, WSClient, WSRequest }
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object DiscoveryXWSClient {
  // #standaloneWSClient
  def standaloneWSClient()(implicit system: ActorSystem[_]): DiscoveryXStandaloneWSClient =
    standaloneWSClient(StandaloneAhcWSClient())

  def standaloneWSClient(client: StandaloneWSClient)(implicit system: ActorSystem[_]): DiscoveryXStandaloneWSClient =
    new DiscoveryXStandaloneWSClient(client)
  // #standaloneWSClient

  // #wsClient
  def wsClient()(implicit system: ActorSystem[_]): DiscoveryXPlayWSClient = wsClient(AhcWSClient())

  def wsClient(client: WSClient)(implicit system: ActorSystem[_]): DiscoveryXPlayWSClient =
    new DiscoveryXPlayWSClient(client)(system)

  def wsClient(asyncHttpClient: AsyncHttpClient)(implicit system: ActorSystem[_]): DiscoveryXPlayWSClient =
    wsClient(new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient)))
  // #wsClient
}

final class DiscoveryXPlayWSClient(client: WSClient)(implicit system: ActorSystem[_]) extends WSClient {
  import system.executionContext
  val namingClient: NamingClient = DefaultNamingClient(system)

  override def underlying[T]: T = client.underlying

  override def url(uri: String): WSRequest = url(uri, namingClient.settings.queryTimeout)

  def url(uri: String, timeout: FiniteDuration): WSRequest = Await.result(asyncUrl(uri), timeout)

  def asyncUrl(url: String): Future[WSRequest] =
    namingClient.generateUri(url).map(uri => client.url(uri.map(_.toString()).getOrElse(url)))

  override def close(): Unit = client.close()
}

final class DiscoveryXStandaloneWSClient(client: StandaloneWSClient)(implicit system: ActorSystem[_])
    extends StandaloneWSClient {
  import system.executionContext
  val namingClient: NamingClient = DefaultNamingClient(system)

  override def underlying[T]: T = client.underlying

  override def url(uri: String): StandaloneWSRequest = url(uri, namingClient.settings.queryTimeout)

  def url(uri: String, timeout: FiniteDuration): StandaloneWSRequest = Await.result(asyncUrl(uri), timeout)

  def asyncUrl(url: String): Future[StandaloneWSRequest] =
    namingClient.generateUri(url).map(uri => client.url(uri.map(_.toString()).getOrElse(url)))

  override def close(): Unit = client.close()
}

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

package fusion.discoveryx.server.naming.internal

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import akka.io.{ IO, UdpConnected }
import akka.stream.scaladsl.{ Keep, Sink, Source, Tcp }
import akka.stream.{ CompletionStrategy, OverflowStrategy }
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future

object SniffUtils extends StrictLogging {
  def sniffTcp(useTls: Boolean, ip: String, port: Int)(implicit system: ActorSystem[_]): Future[Boolean] = {
    // TODO SSLEngine 的创建方式应可优化
    val connection = if (useTls) {
      Tcp(system).outgoingConnectionWithTls(
        InetSocketAddress.createUnresolved(ip, port),
        () => {
          val engine = Http(system).defaultClientHttpsContext.sslContext.createSSLEngine()
          engine.setUseClientMode(true)
          engine
        })
    } else {
      Tcp(system).outgoingConnection(ip, port)
    }

    Source.single(ByteString.empty).via(connection).map(_ => true).runWith(Sink.head)
  }

  def sniffUdp(ip: String, port: Int)(implicit system: ActorSystem[_]): Future[Boolean] = {
    val udp = IO(UdpConnected)(system.toClassic)

    val (handler, future) = Source
      .actorRef[UdpConnected.Event](completionMatcher(udp), failureMatcher, 2, OverflowStrategy.dropHead)
      .toMat(Sink.ignore)(Keep.both)
      .run()

    udp ! UdpConnected.Connect(handler, InetSocketAddress.createUnresolved(ip, port))

    future.map(_ => true)(system.executionContext)
  }
  private def completionMatcher(udp: ActorRef): PartialFunction[Any, CompletionStrategy] = {
    case UdpConnected.Connected =>
      udp ! UdpConnected.Disconnect
      CompletionStrategy.immediately
  }
  private def failureMatcher: PartialFunction[Any, Throwable] = {
    case UdpConnected.Disconnected => new IllegalStateException()
  }

  def sniffHttp(useTls: Boolean, ip: String, port: Int, httpPath: String)(
      implicit system: ActorSystem[_]): Future[Boolean] = {
    import system.executionContext
    val protocol = if (useTls) "https" else "http"
    val path = if (httpPath.isEmpty || httpPath.head != '/') s"/$httpPath" else httpPath
    Http(system).singleRequest(HttpRequest(HttpMethods.GET, s"$protocol://$ip:$port$path")).map(_.status.isSuccess())
  }
}

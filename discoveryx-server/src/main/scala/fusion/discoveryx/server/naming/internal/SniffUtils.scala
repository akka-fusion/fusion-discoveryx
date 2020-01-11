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

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, Uri }
import akka.stream.scaladsl.{ Sink, Source, Tcp }
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future

object SniffUtils extends StrictLogging {
  /**
   * TODO tls unavailable
   */
  def sniffTcp(useTls: Boolean, ip: String, port: Int)(implicit system: ActorSystem[_]): Future[Boolean] = {
    // TODO SSLEngine 的创建方式应可优化
    val connection = if (useTls) {
      Tcp(system).outgoingConnectionWithTls(InetSocketAddress.createUnresolved(ip, port), () => {
        val engine = Http(system).defaultClientHttpsContext.sslContext.createSSLEngine()
        engine.setUseClientMode(true)
        engine
      })
    } else {
      Tcp(system).outgoingConnection(ip, port)
    }

    Source.single(ByteString.empty).via(connection).runWith(Sink.ignore).map(_ => true)(system.executionContext)
  }

//  /**
//   * TODO unavailable
//   */
//  def sniffUdp(ip: String, port: Int)(implicit system: ActorSystem[_]): Future[Boolean] = {
//    val udp = IO(UdpConnected)(system.toClassic)
//
//    val (handler, source) = Source
//      .actorRef[UdpConnected.Event](completionMatcher(udp), failureMatcher, 2, OverflowStrategy.dropHead)
//      .preMaterialize()
//
//    val future = source.runWith(Sink.ignore)
//
//    udp ! UdpConnected.Connect(handler, InetSocketAddress.createUnresolved(ip, port))
//
//    future.map(_ => true)(system.executionContext)
//  }
//  private def completionMatcher(udp: ActorRef): PartialFunction[Any, CompletionStrategy] = {
//    case message: UdpConnected.Message =>
//      logger.debug(s"Receive UDP message: $message.")
//      udp ! UdpConnected.Disconnect
//      CompletionStrategy.immediately
//    case message =>
//      logger.debug(s"Receive message: $message.")
//      CompletionStrategy.immediately
//  }
//  private def failureMatcher: PartialFunction[Any, Throwable] = {
//    case UdpConnected.Disconnected => new IllegalStateException()
//  }

  def sniffHttp(useTls: Boolean, ip: String, port: Int, httpPath: String)(
      implicit system: ActorSystem[_]): Future[Boolean] = {
    import system.executionContext
    val protocol = if (useTls) "https" else "http"
    val path = if (httpPath.isEmpty || httpPath.head != '/') s"/$httpPath" else httpPath
    Http(system).singleRequest(HttpRequest(HttpMethods.GET, s"$protocol://$ip:$port$path")).map(_.status.isSuccess())
  }

  def sniffHttp(uri: Uri)(implicit system: ActorSystem[_]): Future[Boolean] = {
    import system.executionContext
    Http(system).singleRequest(HttpRequest(HttpMethods.GET, uri)).map(_.status.isSuccess())
  }
}

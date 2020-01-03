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

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import akka.stream.scaladsl.Tcp

import scala.concurrent.Future

object SniffUtils {
  def sniffTcp(ip: String, port: Int)(implicit system: ActorSystem[_]): Future[Boolean] = {
    Tcp(system).bind(ip, port)
    ???
  }

  def sniffUdp(ip: String, port: Int)(implicit system: ActorSystem[_]): Future[Boolean] = {
    ???
  }

  def sniffHttp(protocol: String, ip: String, port: Int, httpPath: String)(
      implicit system: ActorSystem[_]): Future[Boolean] = {
    import system.executionContext
    val path = if (httpPath.isEmpty || httpPath.head != '/') s"/$httpPath" else httpPath
    Http(system).singleRequest(HttpRequest(HttpMethods.GET, s"$protocol://$ip:$port$path")).map(_.status.isSuccess())
  }
}

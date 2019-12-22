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

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import fusion.common.FusionProtocol

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object NamingClientDemo {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(FusionProtocol.behavior, "NamingClient", ConfigFactory.load("application-local.conf"))
    val settings = NamingClientSettings(system)
    val namingClient = DiscoveryXNamingClient(settings, system)

    val f = namingClient.registerOnSettings()
    val reply = Await.result(f, Duration.Inf)
    println(reply)
  }
}

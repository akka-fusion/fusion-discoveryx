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

import akka.actor.typed.{ ActorSystem, ExtensionId }

import scala.util.{ Failure, Success }

object DefaultNamingClient extends ExtensionId[NamingClient] {
  override def createExtension(system: ActorSystem[_]): NamingClient = {
    val client = NamingClient(system)
    if (NamingClientSettings(system).autoRegistration) {
      client
        .registerOnSettings()
        .onComplete {
          case Success(value)     => system.log.info(s"Auto registration success, return is $value.")
          case Failure(exception) => system.log.error("Auto registration failure.", exception)
        }(system.executionContext)
    }
    client
  }
}

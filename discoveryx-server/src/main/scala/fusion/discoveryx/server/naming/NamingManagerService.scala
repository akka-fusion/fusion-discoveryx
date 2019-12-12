/*
 * Copyright 2019 helloscala.com
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

package fusion.discoveryx.server.naming

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import fusion.discoveryx.server.protocol.NamingManagerCommand.Cmd
import fusion.discoveryx.server.protocol.{ GetService, ListService, NamingManagerCommand, NamingResponse }

import scala.concurrent.Future
import scala.concurrent.duration._

class NamingManagerService()(implicit system: ActorSystem[_]) {
  private implicit val timeout: Timeout = 10.seconds
  private val namingProxy: ActorRef[ShardingEnvelope[NamingManager.Command]] = NamingManager.init(system)

  def listService(cmd: ListService): Future[NamingResponse] = askManager(cmd.namespace, Cmd.ListService(cmd))

  def getService(cmd: GetService): Future[NamingResponse] = askManager(cmd.namespace, Cmd.GetService(cmd))

  @inline private def askManager(namespace: String, cmd: NamingManagerCommand.Cmd): Future[NamingResponse] =
    namingProxy.ask[NamingResponse](replyTo => ShardingEnvelope(namespace, NamingManagerCommand(replyTo, cmd)))
}

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

package fusion.discoveryx.server.naming.service

import java.util.concurrent.TimeoutException

import akka.actor.typed.{ ActorRef, Scheduler }
import akka.cluster.sharding.typed.ShardingEnvelope
import fusion.discoveryx.model.NamingReply
import fusion.discoveryx.server.namespace.NamespaceRef.{ ExistNamespace, NamespaceExists }
import fusion.discoveryx.server.naming.NamingService
import fusion.discoveryx.server.protocol.NamingReplyCommand
import helloscala.common.IntStatus

import scala.concurrent.{ ExecutionContext, Future }
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

trait NamingServiceHelper {
  val serviceInstanceRegion: ActorRef[ShardingEnvelope[NamingService.Command]]
  val namespaceRef: ActorRef[ExistNamespace]

  protected def askNaming(namespace: String, serviceName: String, cmd: NamingReplyCommand.Cmd)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext): Future[NamingReply] = {
    namespaceRef.ask[NamespaceExists](replyTo => ExistNamespace(namespace, replyTo)).flatMap {
      case NamespaceExists(true) =>
        NamingService.makeEntityId(namespace, serviceName) match {
          case Right(entityId) =>
            serviceInstanceRegion
              .ask[NamingReply](replyTo => ShardingEnvelope(entityId, NamingReplyCommand(replyTo, cmd)))
              .recover {
                case _: TimeoutException => NamingReply(IntStatus.GATEWAY_TIMEOUT)
              }
          case Left(errMsg) => Future.successful(NamingReply(IntStatus.INTERNAL_ERROR, errMsg))
        }
      case _ =>
        Future.successful(NamingReply(IntStatus.NOT_FOUND, s"Namespace '$namespace' not exists."))
    }
  }
}

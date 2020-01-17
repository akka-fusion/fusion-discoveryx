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

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import fusion.discoveryx.model.{ InstanceModify, InstanceRemove, NamingReply }
import fusion.discoveryx.server.grpc.NamingManagerService
import fusion.discoveryx.server.namespace.NamespaceRef.{ ExistNamespace, NamespaceExists }
import fusion.discoveryx.server.naming.{ NamingManager, NamingService }
import fusion.discoveryx.server.protocol.NamingManagerCommand.Cmd
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._

class NamingManagerServiceImpl(val namespaceRef: ActorRef[ExistNamespace])(implicit system: ActorSystem[_])
    extends NamingManagerService
    with NamingServiceHelper {
  import system.executionContext
  private implicit val timeout: Timeout = 10.seconds
  override val serviceInstanceRegion = NamingService.init(system)
  private val namingManager = NamingManager.init(system)

  /**
   * #ListService
   */
  override def listService(in: ListService): Future[NamingResponse] = askManager(in.namespace, Cmd.ListService(in))

  /**
   * #GetService
   */
  override def getService(in: GetService): Future[NamingResponse] = askManager(in.namespace, Cmd.GetService(in))

  /**
   * # CreateService
   */
  override def createService(in: CreateService): Future[NamingResponse] =
    askManager(in.namespace, Cmd.CreateService(in))

  /**
   * # ModifyService
   */
  override def modifyService(in: ModifyService): Future[NamingResponse] =
    askManager(in.namespace, Cmd.ModifyService(in))

  /**
   * # RemoveService
   */
  override def removeService(in: RemoveService): Future[NamingResponse] =
    askManager(in.namespace, Cmd.RemoveService(in))

  /**
   * #ModifyInstance
   */
  override def modifyInstance(in: InstanceModify): Future[NamingReply] =
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Modify(in))

  /**
   * #RemoveInstance
   */
  override def removeInstance(in: InstanceRemove): Future[NamingReply] =
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Remove(in))

  @inline private def askManager(namespace: String, cmd: NamingManagerCommand.Cmd): Future[NamingResponse] =
    namespaceRef
      .ask[NamespaceExists](replyTo => ExistNamespace(namespace, replyTo))
      .flatMap {
        case NamespaceExists(true) =>
          namingManager.ask[NamingResponse](replyTo => ShardingEnvelope(namespace, NamingManagerCommand(replyTo, cmd)))
        case _ =>
          Future.successful(NamingResponse(IntStatus.NOT_FOUND, s"Namespace [$namespace] not found."))
      }(system.executionContext)
}

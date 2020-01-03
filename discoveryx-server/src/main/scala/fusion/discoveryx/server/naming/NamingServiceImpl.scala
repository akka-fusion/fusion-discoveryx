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

package fusion.discoveryx.server.naming

import java.util.concurrent.TimeoutException

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.grpc.scaladsl.Metadata
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.common.Headers
import fusion.discoveryx.grpc.NamingServicePowerApi
import fusion.discoveryx.model._
import fusion.discoveryx.server.management.NamespaceRef.{ ExistNamespace, NamespaceExists }
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.exception.{ HSBadRequestException, HSInternalErrorException }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

class NamingServiceImpl(namespaceRef: ActorRef[ExistNamespace])(implicit system: ActorSystem[_])
    extends NamingServicePowerApi
    with StrictLogging {
  import system.executionContext
  implicit private val timeout: Timeout = 5.seconds
  private val serviceInstanceRegion = NamingService.init(system)

  /**
   * 查询服务状态
   */
  override def serverStatus(in: ServerStatusQuery, metadata: Metadata): Future[ServerStatusBO] = {
    Future.successful(ServerStatusBO(IntStatus.OK))
  }

  /**
   * 添加实例
   */
  override def registerInstance(in: InstanceRegister, metadata: Metadata): Future[NamingReply] = {
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Register(in))
  }

  /**
   * 修改实例
   */
  override def modifyInstance(in: InstanceModify, metadata: Metadata): Future[NamingReply] = {
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Modify(in))
  }

  /**
   * 删除实例
   */
  override def removeInstance(in: InstanceRemove, metadata: Metadata): Future[NamingReply] = {
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Remove(in))
  }

  /**
   * 查询实例
   */
  override def queryInstance(in: InstanceQuery, metadata: Metadata): Future[NamingReply] = {
    askNaming(in.namespace, in.serviceName, NamingReplyCommand.Cmd.Query(in))
  }

  override def listenerService(in: ServiceListener, metadata: Metadata): Source[ServiceEvent, NotUsed] = {
    try {
      val entityId = NamingService.makeEntityId(in.namespace, in.serviceName) match {
        case Right(value) => value
        case Left(errMsg) => throw HSBadRequestException(errMsg)
      }

      val (ref, source) = ActorSource
        .actorRef[NamingService.Event]({
          case _: ServiceEventStop =>
        }, { changed =>
          throw HSInternalErrorException(s"Throw error: $changed.")
        }, 2, OverflowStrategy.dropHead)
        .preMaterialize()
      serviceInstanceRegion ! ShardingEnvelope(entityId, NamingListenerCommand(ref, in))
      source.mapConcat {
        case evt: NamingServiceEvent => evt.event :: Nil
        case _                       => Nil
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"listenerService($in, $metadata) error: ${e.getLocalizedMessage}")
        Source.empty
    }
  }

  // #heartbeat
  override def heartbeat(
      in: Source[InstanceHeartbeat, NotUsed],
      metadata: Metadata): Source[ServerStatusBO, NotUsed] = {
    try {
      val namespace = metadata
        .getText(Headers.NAMESPACE)
        .getOrElse(throw HSBadRequestException(s"Request header missing, need '${Headers.NAMESPACE}'."))
      val serviceName = metadata
        .getText(Headers.SERVICE_NAME)
        .getOrElse(throw HSBadRequestException(s"Request header missing, need '${Headers.SERVICE_NAME}'."))
      val instanceId = metadata
        .getText(Headers.INSTANCE_ID)
        .getOrElse(throw HSBadRequestException(s"Request header missing, need '${Headers.INSTANCE_ID}'."))
      val entityId = NamingService.makeEntityId(namespace, serviceName) match {
        case Right(value) => value
        case Left(errMsg) => throw HSBadRequestException(errMsg)
      }
      in.map { _ =>
        serviceInstanceRegion ! ShardingEnvelope(entityId, Heartbeat(namespace, serviceName, instanceId))
        ServerStatusBO(IntStatus.OK)
      }
    } catch {
      case e: Exception =>
        logger.warn(s"Receive heartbeat message error: $e")
        Source.single(ServerStatusBO(IntStatus.BAD_REQUEST))
    }
  }
  // #heartbeat

  private def askNaming(namespace: String, serviceName: String, cmd: NamingReplyCommand.Cmd): Future[NamingReply] = {
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

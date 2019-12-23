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

package fusion.discoveryx.client.impl

import akka.NotUsed
import akka.actor.Cancellable
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import fusion.common.extension.FusionCoordinatedShutdown
import fusion.discoveryx.client.{ DiscoveryXNamingClient, NamingClientSettings }
import fusion.discoveryx.common.Headers
import fusion.discoveryx.grpc.NamingServiceClient
import fusion.discoveryx.model._
import helloscala.common.IntStatus

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

private[client] class DiscoveryXNamingClientImpl(
    val settings: NamingClientSettings,
    val serviceClient: NamingServiceClient)(implicit system: ActorSystem[_])
    extends DiscoveryXNamingClient
    with StrictLogging {
  import DiscoveryXNamingClient._
  private var heartbeatInstances = Map[InstanceKey, Cancellable]()
  logger.info(s"DiscoveryXNamingClient instanced: $settings")

  FusionCoordinatedShutdown(system).beforeServiceUnbind("DiscoveryXNamingService") { () =>
    for ((_, cancellable) <- heartbeatInstances if !cancellable.isCancelled) {
      cancellable.cancel
    }
    serviceClient.close()
  }

  override def serverStatus(in: ServerStatusQuery): Future[ServerStatusBO] = serviceClient.serverStatus(in)

  override def registerOnSettings(): Future[NamingReply] =
    try {
      val namespace = settings.namespace.getOrElse(throw new IllegalArgumentException("'namespace' not set."))
      val serviceName = settings.serviceName.getOrElse(throw new IllegalArgumentException("'service-name' not set."))
      val ip = settings.ip.getOrElse(throw new IllegalArgumentException("'ip' not set."))
      val port = settings.port.getOrElse(throw new IllegalArgumentException("'port' not set."))
      val in = InstanceRegister(
        namespace,
        serviceName,
        settings.groupName.getOrElse(""),
        ip,
        port,
        weight = settings.weight,
        health = true,
        enable = settings.enable,
        ephemeral = settings.ephemeral,
        metadata = settings.metadata)
      registerInstance(in)
    } catch {
      case NonFatal(e) => Future.failed(e)
    }

  private def retryRegisterInstance(in: InstanceRegister, delay: FiniteDuration): Unit = {
    import system.executionContext
    system.scheduler.scheduleOnce(delay, () => registerInstance(in))
  }

  override def registerInstance(in: InstanceRegister): Future[NamingReply] = registerInstance(in, 30.seconds)

  def registerInstance(in: InstanceRegister, delay: FiniteDuration): Future[NamingReply] = {
    import system.executionContext
    serviceClient.registerInstance(in).andThen {
      case Success(reply) if reply.status == IntStatus.OK && reply.data.isInstance =>
        val inst = reply.data.instance.get
        val (cancellable, source) =
          Source.tick(settings.heartbeatInterval, settings.heartbeatInterval, InstanceHeartbeat()).preMaterialize()
        val key = InstanceKey(inst)
        heartbeatInstances.get(key).foreach(_.cancel())
        heartbeatInstances = heartbeatInstances.updated(key, cancellable)
        logger.info(s"注册实例成功，开始心跳调度。${settings.heartbeatInterval} | $inst")
        heartbeat(source, inst).runForeach(bo => logger.debug(s"Received heartbeat response: $bo")).failed.foreach {
          e =>
            heartbeatInstancesOnRemoves(key)
            retryRegisterInstance(in, delay)
            logger.error(
              s"Heartbeat connection is abnormally disconnect, try again in $delay. exception throw: ${e.getLocalizedMessage}")
        }
      case Success(reply) =>
        logger.warn(s"注册实例错误，返回：$reply")
      case Failure(exception) =>
        logger.error(s"注册实例失败：$exception")
    }
  }

  private def heartbeatInstancesOnRemoves(key: InstanceKey): Unit = {
    heartbeatInstances.get(key).foreach(_.cancel())
    heartbeatInstances -= key
  }

  override def modifyInstance(in: InstanceModify): Future[NamingReply] = serviceClient.modifyInstance(in)

  override def removeInstance(in: InstanceRemove): Future[NamingReply] = {
    heartbeatInstancesOnRemoves(InstanceKey(in))
    serviceClient.removeInstance(in)
  }

  override def queryInstance(in: InstanceQuery): Future[NamingReply] = serviceClient.queryInstance(in)

  private def heartbeat(in: Source[InstanceHeartbeat, NotUsed], inst: Instance): Source[ServerStatusBO, NotUsed] = {
    serviceClient
      .heartbeat()
      .addHeader(Headers.NAMESPACE, inst.namespace)
      .addHeader(Headers.SERVICE_NAME, inst.serviceName)
      .addHeader(Headers.IP, inst.ip)
      .addHeader(Headers.PORT, Integer.toString(inst.port))
      .addHeader(Headers.INSTANCE_ID, inst.instanceId)
      .invoke(in)
  }
}

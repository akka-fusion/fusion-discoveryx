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

import akka.actor.ExtendedActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.discovery.ServiceDiscovery.{ Resolved, ResolvedTarget }
import akka.discovery.{ Lookup, ServiceDiscovery }
import fusion.discoveryx.model.InstanceQuery

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, Promise }

class DiscoveryXAkkaDiscovery(system: ExtendedActorSystem) extends ServiceDiscovery {
  import system.dispatcher
  private val config =
    system.settings.config.getConfig("akka.discovery." + system.settings.config.getString("akka.discovery.method"))
  private val client =
    NamingClient(NamingClientSettings.fromConfig(system.settings.config, config.getString("setting")), system.toTyped)
  private val namespace = client.settings.namespace
    .getOrElse(throw new ExceptionInInitializerError("Configuration parameter 'namespace' not set."))
  private val groupName = client.settings.groupName.getOrElse("")

  override def lookup(lookup: Lookup, resolveTimeout: FiniteDuration): Future[Resolved] = {
    val in = InstanceQuery(
      namespace,
      lookup.serviceName,
      groupName,
      allHealthy = client.settings.allHealthy,
      oneHealthy = client.settings.oneHealthy)
    val future = client.queryInstance(in).map { reply =>
      reply.data.serviceInfo match {
        case Some(serviceInfo) =>
          val targets = serviceInfo.instances.map(inst => ResolvedTarget(inst.ip, Some(inst.port), None))
          Resolved(lookup.serviceName, targets)
        case None =>
          throw new IllegalAccessException(reply.message)
      }
    }

    if (resolveTimeout.length == 0L) {
      future
    } else {
      val promise = Promise[Resolved]()
      system.scheduler.scheduleOnce(resolveTimeout)(promise.success(Resolved(lookup.serviceName, Nil)))
      Future.firstCompletedOf(List(promise.future, future))
    }
  }
}

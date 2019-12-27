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

package fusion.discoveryx.server.naming.actors

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import fusion.discoveryx.model.{ ServiceInfo, ServiceItem }
import fusion.discoveryx.server.naming.NamingService
import fusion.json.jackson.CborSerializable

case class NamingServiceState(
    instanceIds: Vector[ActorRef[NamingInstance.Command]] = Vector.empty,
    healthyIds: Vector[ActorRef[NamingInstance.Command]] = Vector.empty,
    var unusedIdx: Int = 0,
    serviceItem: ServiceItem = ServiceItem.defaultInstance)
    extends CborSerializable {
  def theChanged(instanceId: ActorRef[NamingInstance.Command], healthy: Boolean): NamingServiceState = {
    val ids = if (instanceIds.contains(instanceId)) instanceIds else instanceIds :+ instanceId
    val healthies =
      if (healthy && !healthyIds.contains(instanceId)) healthyIds :+ instanceId
      else if (!healthy) healthyIds.filterNot(_ == instanceId)
      else healthyIds
    copy(instanceIds = ids, healthyIds = healthies, if (unusedIdx < healthies.length) unusedIdx else 0)
  }

  def containsHealthyId(instanceId: String): Boolean = healthyIds.exists(_.path.name == instanceId)

  def containsInstanceId(instanceId: String): Boolean = instanceIds.exists(_.path.name == instanceId)

  def nextOption(): Option[ActorRef[NamingInstance.Command]] = {
    if (unusedIdx < healthyIds.length) {
      val idx = unusedIdx
      moveUnusedIdx()
      Some(healthyIds(idx))
    } else {
      None
    }
  }

  def moveUnusedIdx(): NamingServiceState = {
    var idx = unusedIdx + 1
    if (idx >= healthyIds.length) {
      idx = 0
    }
    unusedIdx = idx
    this
  }

  def findChild(
      context: ActorContext[NamingService.Command],
      instanceId: String): Option[ActorRef[NamingInstance.Command]] =
    instanceIds.find(_.path.name == instanceId)

  def findHealthyChild(
      context: ActorContext[NamingService.Command],
      instanceId: String): Option[ActorRef[NamingInstance.Command]] =
    healthyIds.find(_.path.name == instanceId)

  def currentServiceInfo(): ServiceInfo =
    ServiceInfo(
      serviceItem.namespace,
      serviceItem.serviceName,
      serviceItem.groupName,
      serviceItem.protectThreshold,
      serviceItem.metadata,
      instanceIds.length,
      healthyIds.length)
}

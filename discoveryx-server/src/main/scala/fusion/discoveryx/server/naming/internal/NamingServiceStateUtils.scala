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

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import fusion.discoveryx.model.{ Instance, ServiceInfo }
import fusion.discoveryx.server.naming.NamingService
import fusion.discoveryx.server.protocol.NamingServiceState

object NamingServiceStateUtils {
  def removeInstance(state: NamingServiceState, instanceId: String): NamingServiceState =
    state.copy(state.instances.filterNot(_.instanceId == instanceId), state.healthIds.filterNot(_ == instanceId))

  def unhealthyInstance(state: NamingServiceState, instanceId: String): NamingServiceState = {
    val idx = state.instances.indexWhere(_.instanceId == instanceId)
    val inst = state.instances(idx)
    val newInst = if (inst.healthy) inst.copy(healthy = false) else inst
    val netInstances = state.instances.updated(idx, newInst)
    state.copy(netInstances, state.healthIds.filterNot(_ == instanceId))
  }

  def theChanged(state: NamingServiceState, inst: Instance, healthy: Boolean): NamingServiceState = {
    val idx = state.instances.indexWhere(_.instanceId == inst.instanceId)

    val newInstances = if (idx < 0) {
      if (inst.ephemeral && !healthy) state.instances
      else state.instances :+ inst
    } else {
      if (inst.ephemeral && !healthy) state.instances.filterNot(_.instanceId == inst.instanceId)
      else state.instances.updated(idx, inst)
    }

    val newHealths = if (healthy && !state.healthIds.contains(inst.instanceId)) {
      if (state.healthIds.contains(inst.instanceId)) state.healthIds
      else state.healthIds :+ inst.instanceId
    } else {
      state.healthIds.filterNot(_ == inst.instanceId)
    }

    state.copy(newInstances, newHealths)
  }

  def containsHealthyId(state: NamingServiceState, instanceId: String): Boolean = state.healthIds.contains(instanceId)

  def containsInstanceId(state: NamingServiceState, instanceId: String): Boolean =
    state.instances.exists(_.instanceId == instanceId)

  def findChild(
      state: NamingServiceState,
      context: ActorContext[NamingService.Command],
      instanceId: String): Option[ActorRef[NamingInstance.Command]] =
    state.instances
      .find(_.instanceId == instanceId)
      .flatMap(inst => context.child(inst.instanceId))
      .map(_.unsafeUpcast[NamingInstance.Command])

  def findHealthyChild(
      state: NamingServiceState,
      context: ActorContext[NamingService.Command],
      instanceId: String): Option[ActorRef[NamingInstance.Command]] =
    state.healthIds.find(_ == instanceId).flatMap(context.child).map(_.unsafeUpcast[NamingInstance.Command])

  def currentServiceInfo(state: NamingServiceState): ServiceInfo =
    ServiceInfo(
      state.serviceItem.namespace,
      state.serviceItem.serviceName,
      state.serviceItem.groupName,
      state.serviceItem.protectThreshold,
      state.serviceItem.metadata,
      state.instances.length,
      state.healthIds.length)
}

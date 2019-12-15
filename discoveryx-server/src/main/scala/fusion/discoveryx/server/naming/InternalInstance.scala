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

import com.typesafe.scalalogging.StrictLogging
import fusion.discoveryx.model.{ Instance, InstanceModify, InstanceQuery }
import fusion.discoveryx.server.protocol.NamingServiceKey

final private[discoveryx] class InternalInstance(private val inst: Instance, settings: NamingSettings)
    extends Ordered[InternalInstance]
    with Equals {
  @transient private val UNHEALTHY_CHECK_THRESHOLD_MILLIS = settings.heartbeatInterval.toMillis + 2000
  @transient val instanceId: String = inst.instanceId

  @transient var lastTickTimestamp: Long = System.currentTimeMillis()

  def healthy: Boolean = (System.currentTimeMillis() - lastTickTimestamp) < UNHEALTHY_CHECK_THRESHOLD_MILLIS

  def refresh(): InternalInstance = {
    lastTickTimestamp = System.currentTimeMillis()
    this
  }

  def withInstance(in: Instance): InternalInstance = new InternalInstance(in, settings)

  def toInstance: Instance = inst.copy(healthy = healthy)

  override def compare(that: InternalInstance): Int = {
    if (that.inst.weight > inst.weight) 1
    else if (that.inst.weight < inst.weight) -1
    else that.inst.instanceId.compare(inst.instanceId)
  }

  override def canEqual(that: Any): Boolean = {
    this == that || (that match {
      case other: InternalInstance => other.inst.instanceId == inst.instanceId
      case _                       => false
    })
  }

  override def equals(obj: Any): Boolean = canEqual(obj)

  override def toString =
    s"InternalInstance(${inst.instanceId}, ${inst.namespace}, ${inst.groupName}, ${inst.serviceName}, ${inst.ip}, ${inst.port}, $healthy, $lastTickTimestamp)"
}

final private[discoveryx] class InternalService(namingServiceKey: NamingServiceKey, settings: NamingSettings)
    extends StrictLogging {
  private var curHealthyIdx = 0
  private var instances = Vector[InternalInstance]()
  private var instIds = Map[String, Int]() // instance id, insts index

  def queryInstance(in: InstanceQuery): Vector[Instance] = {
    logger.debug(s"queryInstance($in); curHealthyIdx: $curHealthyIdx; instIds: $instIds; $instances")
    val selects =
      if (in.allHealthy) allHealthy()
      else if (in.oneHealthy) oneHealthy()
      else allInstance()
    selects.map(_.toInstance)
  }

  def addInstance(inst: Instance): Instance = {
    val internalInstance = new InternalInstance(inst, settings)
    val items = instIds.get(inst.instanceId) match {
      case Some(idx) => instances.updated(idx, internalInstance)
      case None      => internalInstance +: instances
    }
    saveInstances(items)
    logger.debug(s"addInstance($inst) after; curHealthyIdx: $curHealthyIdx; instIds: $instIds; $instances")
    internalInstance.toInstance
  }

  def modifyInstance(in: InstanceModify): Option[Instance] = {
    instIds.get(in.instanceId).map { idx =>
      val internal = instances(idx)
      val old = internal.toInstance
      val inst = old.copy(
        groupName = in.groupName.getOrElse(old.groupName),
        ip = in.ip.getOrElse(old.ip),
        port = in.port.getOrElse(old.port),
        weight = in.weight.getOrElse(old.weight),
        healthy = in.health.getOrElse(old.healthy),
        enabled = in.enable.getOrElse(old.enabled),
        metadata = if (in.replaceMetadata) in.metadata else old.metadata ++ in.metadata)
      saveInstances(instances.updated(idx, internal.withInstance(inst)))
      inst
    }
  }

  def removeInstance(instId: String): Boolean = {
    if (instIds.contains(instId)) {
      saveInstances(instances.filterNot(_.instanceId == instId))
      true
    } else {
      false
    }
  }

  def allInstance(): Vector[InternalInstance] = instances

  def allRealInstance(): Vector[Instance] = allInstance().map(_.toInstance)

  def allHealthy(): Vector[InternalInstance] = instances.filter(_.healthy)

  def oneHealthy(): Vector[InternalInstance] = {
    val healths = allHealthy()
    if (healths.isEmpty) {
      curHealthyIdx = 0
      healths
    } else if (curHealthyIdx < healths.size) {
      val ret = healths(curHealthyIdx)
      curHealthyIdx += 1
      Vector(ret)
    } else {
      curHealthyIdx = if (healths.size == 1) 0 else 1
      Vector(healths.head)
    }
  }

  def processHeartbeat(instanceId: String): InternalService = {
    instIds.get(instanceId) match {
      case Some(idx) =>
        val inst = instances(idx).refresh()
        logger.debug(s"Successfully processed heartbeat request, instance: $inst.")
      case None => logger.warn(s"Service not registered, instanceId: $instanceId.")
    }
    this
  }

  def checkHealthy(): InternalService = {
    for (inst <- instances if !inst.healthy) {
      // TODO alarm or remove unhealthy instance ?
      logger.warn(s"Instance unhealthy, is $inst")
    }
    this
  }

  private def saveInstances(items: Vector[InternalInstance]): Unit = {
    this.instances = items.sortWith(_ > _)
    instIds = this.instances.view.map(_.instanceId).zipWithIndex.toMap
  }
}

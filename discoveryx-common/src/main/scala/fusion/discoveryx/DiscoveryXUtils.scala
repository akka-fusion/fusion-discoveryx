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

package fusion.discoveryx

import fusion.discoveryx.common.{ Constants, Protocols }
import fusion.discoveryx.model.{ HealthyCheckMethod, Instance, InstanceModify, InstanceRegister }
import helloscala.common.util.{ DigestUtils, StringUtils }

object DiscoveryXUtils {
  def makeInstanceId(namespace: String, serviceName: String, ip: String, port: Int): String = {
    require(StringUtils.isNoneBlank(namespace), s"namespace invalid, is: $namespace")
    require(StringUtils.isNoneBlank(serviceName), s"serviceName invalid, is: $serviceName")
    require(StringUtils.isNoneBlank(ip), s"ip invalid, is: $ip")
    require(port > 0, s"port invalid: is: $port")
    DigestUtils.sha1Hex(namespace + serviceName + ip + port)
  }

  def toInstance(in: InstanceRegister): Instance =
    formatInstance(
      Instance(
        makeInstanceId(in.namespace, in.serviceName, in.ip, in.port),
        in.namespace,
        in.serviceName,
        in.groupName,
        in.ip,
        in.port,
        in.weight,
        in.health,
        in.enable,
        in.ephemeral,
        in.metadata,
        in.healthyCheckMethod,
        in.healthyCheckInterval,
        in.unhealthyCheckCount,
        in.protocol))

  def formatInstance(inst: Instance): Instance =
    inst.copy(
      serviceName = if (inst.serviceName.isEmpty) Constants.DEFAULT_GROUP_NAME else inst.serviceName,
      healthyCheckMethod =
        if (inst.healthyCheckMethod == HealthyCheckMethod.NOT_SET) HealthyCheckMethod.CLIENT_REPORT
        else inst.healthyCheckMethod,
      unhealthyCheckCount = if (inst.unhealthyCheckCount < 1) 1 else inst.unhealthyCheckCount,
      protocol = Protocols.formatProtocol(inst.protocol))

  def instanceModify(old: Instance, in: InstanceModify): Instance = {
    old.copy(
      groupName = in.groupName.getOrElse(old.groupName),
      ip = in.ip.getOrElse(old.ip),
      port = in.port.getOrElse(old.port),
      weight = in.weight.getOrElse(old.weight),
      healthy = in.health.getOrElse(old.healthy),
      enabled = in.enable.getOrElse(old.enabled),
      metadata = if (in.replaceMetadata) in.metadata else old.metadata ++ in.metadata,
      healthyCheckMethod =
        if (in.healthyCheckMethod == HealthyCheckMethod.NOT_SET || in.healthyCheckMethod.isUnrecognized)
          old.healthyCheckMethod
        else in.healthyCheckMethod,
      healthyCheckInterval = in.healthyCheckInterval.getOrElse(old.healthyCheckInterval),
      unhealthyCheckCount = in.unhealthyCheckCount.getOrElse(old.unhealthyCheckCount),
      protocol = in.protocol.map(Protocols.formatProtocol).getOrElse(old.protocol))
  }
  def userHome: Option[String] = sys.env.get("HOME") orElse sys.props.get("user.home")
}

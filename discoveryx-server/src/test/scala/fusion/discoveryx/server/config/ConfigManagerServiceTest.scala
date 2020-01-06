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

package fusion.discoveryx.server.config

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.fusion.testkit.FusionActorTestKit
import fusion.discoveryx.server.config.service.ConfigManagerServiceImpl
import fusion.discoveryx.server.namespace.NamespaceRef.ExistNamespace
import fusion.discoveryx.server.namespace.{ NamespaceManager, NamespaceRef }
import fusion.discoveryx.server.protocol.ListConfig
import fusion.discoveryx.server.util.ProtobufJson4s
import helloscala.common.IntStatus
import org.scalatest.WordSpecLike

class ConfigManagerServiceTest extends ScalaTestWithActorTestKit(FusionActorTestKit()) with WordSpecLike {
  NamespaceManager.init(system)
  private val namespaceRef = spawn(NamespaceRef()).narrow[ExistNamespace]
  private val configManagerService = new ConfigManagerServiceImpl(namespaceRef)

  "ConfigManagerService" must {
    val namespace = "me.yangbajing"

    "listConfig" in {
      val resp = configManagerService.listConfig(ListConfig(namespace)).futureValue
      resp.status shouldBe IntStatus.OK
      println(ProtobufJson4s.toJsonPrettyString(resp))
    }
  }
}

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

package fusion.discoveryx.server.management

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.grpc.GrpcClientSettings
import akka.stream.SystemMaterializer
import com.typesafe.config.ConfigFactory
import fusion.core.extension.FusionCore
import fusion.discoveryx.server.grpc.{ UserService, UserServiceClient }
import fusion.discoveryx.server.protocol.CreateUser
import fusion.discoveryx.server.util.ProtobufJson4s
import org.scalatest.WordSpecLike

class UserServiceClientTest
    extends ScalaTestWithActorTestKit(ConfigFactory.load("application-test.conf"))
    with WordSpecLike {
  "UserServiceClient" must {
    implicit val classicSystem = FusionCore(system).classicSystem
    implicit val mat = SystemMaterializer(system).materializer
    implicit val ec = system.executionContext
    val client = UserServiceClient(GrpcClientSettings.fromConfig(UserService.name))

    "CreateUser" in {
      val resp = client.createUser(CreateUser("yangbajing", "yangbajing", "Fusion DiscoveryX User")).futureValue
      println(ProtobufJson4s.toJsonPrettyString(resp))
    }
  }
}

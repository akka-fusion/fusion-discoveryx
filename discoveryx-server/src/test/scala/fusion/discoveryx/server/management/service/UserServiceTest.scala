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

package fusion.discoveryx.server.management.service

import akka.actor.testkit.typed.scaladsl.{ ActorTestKit, ScalaTestWithActorTestKit }
import com.typesafe.config.ConfigFactory
import fusion.common.config.FusionConfigFactory
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.management.{ UserEntity, UserManager }
import fusion.discoveryx.server.protocol.{ CreateUser, Login, UserRole }
import fusion.discoveryx.server.util.ProtobufJson4s
import helloscala.common.IntStatus
import org.scalatest.WordSpecLike

class UserServiceTest
    extends ScalaTestWithActorTestKit(
      ActorTestKit(
        Constants.DISCOVERYX,
        FusionConfigFactory
          .arrangeConfig(ConfigFactory.load("application-test.conf"), Constants.DISCOVERYX, Seq("akka"))))
    with WordSpecLike {
  private val userService = new UserServiceImpl(UserEntity.init(system), UserManager.init(system))

  "UserServiceTest" should {
    "removeUser" in {}

    "logout" in {}

    "createUser" in {
      val in = CreateUser("discoveryx", "discoveryx", "DiscoveryX Administrator", UserRole.ADMIN)
      val response = userService.createUser(in).futureValue
      println(ProtobufJson4s.toJsonPrettyString(response))
      response.status should be(IntStatus.OK)
    }

    "modifyUser" in {}

    "listUser" in {}

    "login" in {
      val response = userService.login(Login("discoveryx", "discoveryx")).futureValue
      println(ProtobufJson4s.toJsonPrettyString(response))
      response.status should be(IntStatus.OK)
    }
  }
}

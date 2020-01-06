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

package akka.fusion.testkit

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.typesafe.config.ConfigFactory
import fusion.common.config.FusionConfigFactory
import fusion.discoveryx.common.Constants

object FusionActorTestKit {
  def apply(): ActorTestKit =
    new ActorTestKit(
      name = Constants.DISCOVERYX,
      config = FusionConfigFactory.arrangeConfig(ConfigFactory.load("application-test"), Constants.DISCOVERYX),
      settings = None)
}

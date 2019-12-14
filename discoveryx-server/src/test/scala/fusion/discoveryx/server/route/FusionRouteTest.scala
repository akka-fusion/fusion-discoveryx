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

package fusion.discoveryx.server.route

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.testkit.TestDuration
import com.typesafe.config.{ Config, ConfigFactory }
import fusion.core.extension.FusionCore
import fusion.discoveryx.DiscoveryX
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Span }
import org.scalatest.{ EitherValues, Matchers, OptionValues, Suite }

import scala.concurrent.duration._

trait FusionRouteTest extends ScalatestRouteTest with Matchers with OptionValues with EitherValues with ScalaFutures {
  this: Suite =>
  protected implicit val timeout: RouteTestTimeout = RouteTestTimeout(5.seconds.dilated)
  protected var discoveryX: DiscoveryX = _

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(scaled(Span(10.second.toMillis, Millis)), scaled(Span(15, Millis)))

  override protected def createActorSystem(): ActorSystem = {
    discoveryX = DiscoveryX.fromOriginalConfig(ConfigFactory.load("application-test.conf"))
    FusionCore(discoveryX.system)
    discoveryX.classicSystem
  }

  override def testConfig: Config = discoveryX.config
}

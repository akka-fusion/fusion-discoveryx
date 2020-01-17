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

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.FunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration._

class SniffUtilsTest extends ScalaTestWithActorTestKit with FunSuiteLike {
  test("testSniffTcp") {
    val result = Await.result(SniffUtils.sniffTcp(false, "114.67.71.244", 80), 1.minute)
    println(result)
    result shouldBe true
  }

  test("testSniffUdp") {
    val result = SniffUtils.sniffUdp("localhost", 137).futureValue
    result shouldBe true
  }

  test("testSniffHttp") {
    val result = SniffUtils.sniffHttp("http://gitee.com").futureValue
    result shouldBe true
  }

  test("testSniffHttps") {
    val result = SniffUtils.sniffHttp("https://gitee.com").futureValue
    result shouldBe true
  }
}

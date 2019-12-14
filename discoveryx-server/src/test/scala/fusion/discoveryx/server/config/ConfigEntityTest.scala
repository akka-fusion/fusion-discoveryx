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

import akka.actor.testkit.typed.scaladsl.{ ActorTestKit, ScalaTestWithActorTestKit }
import akka.cluster.sharding.typed.ShardingEnvelope
import fusion.discoveryx.model.{ ConfigPublish, ConfigReply }
import fusion.discoveryx.server.protocol.PublishConfig
import org.scalatest.WordSpecLike
import akka.actor.typed.scaladsl.AskPattern._
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.util.ProtobufJson4s
import helloscala.common.config.FusionConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class ConfigEntityTest
    extends ScalaTestWithActorTestKit(
      ActorTestKit(
        Constants.DISCOVERYX,
        FusionConfigFactory.arrangeConfig(ConfigFactory.load("application-test.conf"), Constants.DISCOVERYX, "akka")))
    with WordSpecLike {
  override implicit def timeout: Timeout = 10.seconds
  private val configEntity = ConfigEntity.init(system)

  "ConfigEntity" must {
    val namespace = "me.yangbajing"
    val dataId = "akka"
    val content =
      """discoveryx {
      |  server {
      |    enable = true
      |  }
      |}""".stripMargin

    "init" in {
      val readJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
      val f = readJournal.currentPersistenceIds().runWith(Sink.seq)
      val ids = Await.result(f, 20.seconds)
      println(ids)
    }

    "publishConfig" in {
      println(system.settings.config.getObject("akka-persistence-jdbc.shared-databases"))
      val future = configEntity.ask[ConfigReply] { replyTo =>
        ShardingEnvelope(
          ConfigEntity.ConfigKey.makeEntityId(namespace, dataId),
          PublishConfig(ConfigPublish(namespace, dataId, content = content), replyTo))
      }
      val reply = Await.result(future, Duration.Inf)
      println(ProtobufJson4s.toJsonPrettyString(reply))
    }
  }
}

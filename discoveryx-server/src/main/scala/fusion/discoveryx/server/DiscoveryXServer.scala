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

package fusion.discoveryx.server

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.{ actor => classic }
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging
import fusion.common.config.FusionConfigFactory
import fusion.core.extension.FusionCore
import fusion.discoveryx.DiscoveryX
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.route.Routes
import helloscala.common.Configuration
import slick.jdbc.H2Profile

import scala.util.{ Failure, Success }

class DiscoveryXServer private (discoveryX: DiscoveryX) extends StrictLogging {
  FusionCore(discoveryX.system)

  def start(): Unit = {
    import DiscoveryXServer._
    checkDatabase(discoveryX.config)
    startHttp()(discoveryX.classicSystem)
  }

  private def startHttp()(implicit system: classic.ActorSystem): Unit = {
    val route = new Routes(discoveryX).route
    val config = discoveryX.config
    Http()
      .bindAndHandleAsync(
        Route.asyncHandler(route),
        config.getString("fusion.http.default.server.host"),
        config.getInt("fusion.http.default.server.port"))
      .onComplete {
        case Success(value)     => logger.info(s"HTTP started, bind to $value")
        case Failure(exception) => logger.error(s"HTTP start failure. $exception")
      }(system.dispatcher)
  }
}

object DiscoveryXServer {
  def apply(discoveryX: DiscoveryX): DiscoveryXServer = new DiscoveryXServer(discoveryX)
  def apply(config: Config): DiscoveryXServer = apply(DiscoveryX.fromMergedConfig(config))
  def apply(): DiscoveryXServer =
    apply(FusionConfigFactory.arrangeConfig(ConfigFactory.load(), Constants.DISCOVERYX))

  private val H2_CREATE_SQL = """CREATE TABLE IF NOT EXISTS public."journal" (
                                |  "ordering" BIGINT AUTO_INCREMENT,
                                |  "persistence_id" VARCHAR(255) NOT NULL,
                                |  "sequence_number" BIGINT NOT NULL,
                                |  "deleted" BOOLEAN DEFAULT FALSE NOT NULL,
                                |  "tags" VARCHAR(255) DEFAULT NULL,
                                |  "message" BYTEA NOT NULL,
                                |  PRIMARY KEY("persistence_id", "sequence_number")
                                |);
                                |
                                |CREATE UNIQUE INDEX IF NOT EXISTS "journal_ordering_idx" ON public."journal"("ordering");
                                |
                                |CREATE TABLE IF NOT EXISTS public."snapshot" (
                                |  "persistence_id" VARCHAR(255) NOT NULL,
                                |  "sequence_number" BIGINT NOT NULL,
                                |  "created" BIGINT NOT NULL,
                                |  "snapshot" BYTEA NOT NULL,
                                |  PRIMARY KEY("persistence_id", "sequence_number")
                                |);""".stripMargin

  def checkDatabase(config: Config): Unit = {
    val c = Configuration(config)
    if (c.get[Option[String]]("akka.persistence.journal.plugin").exists(_.startsWith("jdbc")) &&
        c.get[Option[String]]("akka-persistence-jdbc.shared-databases.slick.profile")
          .contains("slick.jdbc.H2Profile$")) {
      import H2Profile.api._
      val db = Database.forConfig("akka-persistence-jdbc.shared-databases.slick.db", c.underlying)
      val session = db.createSession()
      try {
        session.withStatement() { stmt =>
          stmt.execute(H2_CREATE_SQL)
        }
      } finally {
        session.close()
        db.close()
      }
    }
  }
}

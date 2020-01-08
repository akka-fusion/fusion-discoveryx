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

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.{ actor => classic }
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging
import fusion.common.config.FusionConfigFactory
import fusion.core.extension.FusionCore
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.protocol.{ CreateUser, GetUser, UserResponse, UserRole }
import fusion.discoveryx.server.route.Routes
import fusion.discoveryx.server.user.{ UserEntity, UserManager }
import fusion.discoveryx.server.user.service.UserServiceImpl
import helloscala.common.{ Configuration, IntStatus }
import helloscala.common.util.Utils
import slick.jdbc.H2Profile

import scala.concurrent.Future
import scala.io.BufferedSource
import scala.util.{ Failure, Success }

class DiscoveryXServer private (discoveryX: DiscoveryX) extends StrictLogging {
  FusionCore(discoveryX.system)

  def start(): Future[Http.ServerBinding] = {
    import DiscoveryXServer._
    tryCheckAndInitRDBMS(discoveryX.config)
    checkAndInitDefaultUser()(discoveryX.system)
    startHttp()(discoveryX.classicSystem)
  }

  private def startHttp()(implicit system: classic.ActorSystem): Future[Http.ServerBinding] = {
    val route = new Routes(discoveryX).init().route
    val config = discoveryX.config
    val f = Http().bindAndHandleAsync(
      Route.asyncHandler(route),
      config.getString("fusion.http.default.server.host"),
      config.getInt("fusion.http.default.server.port"))
    f.onComplete {
      case Success(value)     => logger.info(s"HTTP started, bind to $value")
      case Failure(exception) => logger.error(s"HTTP start failure. $exception")
    }(system.dispatcher)
    f
  }
}

object DiscoveryXServer extends StrictLogging {
  def apply(discoveryX: DiscoveryX): DiscoveryXServer = new DiscoveryXServer(discoveryX)
  def apply(config: Config): DiscoveryXServer = apply(DiscoveryX.fromMergedConfig(config))
  def apply(): DiscoveryXServer = apply(FusionConfigFactory.arrangeConfig(ConfigFactory.load(), Constants.DISCOVERYX))

  def tryCheckAndInitRDBMS(config: Config): Unit = {
    val c = Configuration(config)

    if (c.get[Option[String]]("akka.persistence.journal.plugin").exists(_.startsWith("jdbc"))) {
      val dbConfig = c
        .get[Option[String]]("jdbc-journal.use-shared-db")
        .flatMap(db => c.get[Option[Configuration]](s"akka-persistence-jdbc.shared-databases.$db"))

      for {
        cc <- dbConfig
        buffer <- schemaReader(cc.getOrElse("profile", ""))
      } {
        import H2Profile.api._
        val db = Database.forConfig("db", cc.underlying)
        val session = db.createSession()
        try {
          session.withStatement() { stmt =>
            val sql = buffer.getLines().mkString("\n")
            logger.whenDebugEnabled {
              logger.debug(s"Init Database Schemas from Config: $cc\nSQL: $sql")
            }
            stmt.execute(sql)
          }
        } finally {
          Utils.closeQuiet(session)
          Utils.closeQuiet(db)
          Utils.closeQuiet(buffer)
        }
      }
    }
  }

  private def checkAndInitDefaultUser()(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    val userService = new UserServiceImpl(UserEntity.init(system), UserManager.init(system))
    userService
      .getUser(GetUser(Constants.DISCOVERYX))
      .recover { case _ => UserResponse(IntStatus.INTERNAL_ERROR) }
      .foreach {
        case resp if IntStatus.isSuccess(resp.status) =>
          logger.info(s"The default account [${Constants.DISCOVERYX}] already exist.")
        case _ =>
          userService
            .createUser(
              CreateUser(
                Constants.DISCOVERYX,
                Constants.DISCOVERYX,
                "Fusion DiscoveryX Administrator.",
                UserRole.ADMIN))
            .onComplete {
              case Success(value) =>
                logger.info(s"Initialization default user ${Constants.DISCOVERYX} successful, return is $value.")
              case Failure(e) =>
                logger.warn(s"Initialization default user ${Constants.DISCOVERYX} failure, exception: ${e.toString}")
            }
      }
  }

  private def schemaReader(profile: String): Option[BufferedSource] =
    Option(profile match {
      case "slick.jdbc.H2Profile$" => scala.io.Source.fromResource("sql/schemas/h2.sql")
      case _                       => null
    })
}

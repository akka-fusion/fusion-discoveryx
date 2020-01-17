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
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import com.typesafe.scalalogging.StrictLogging

class DiscoveryPersistenceQuery private (system: ActorSystem[_]) extends StrictLogging {
  def readJournal: DiscoveryXReadJournal = {
    val reader = system.settings.config.getString("akka.persistence.journal.plugin") match {
      case s if s.startsWith("cassandra") =>
        PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
      case s if s.startsWith("jdbc") =>
        PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
      case s =>
        throw new ExceptionInInitializerError(
          s"The configuration key `akka-persistence` has an invalid value [$s], only support `cassandra-journal`, `jdbc-journal`.")
    }
    logger.debug(s"ReadJournal is [$reader].")
    reader
  }
}

object DiscoveryPersistenceQuery {
  def apply(system: ActorSystem[_]): DiscoveryPersistenceQuery = new DiscoveryPersistenceQuery(system)
}

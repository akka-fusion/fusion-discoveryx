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

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.server.protocol.UserManagerCommand.Cmd
import fusion.discoveryx.server.protocol.UserResponse.Data
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

object UserManager {
  trait Command extends UserEntity.Command
  trait Event

  val NAME = "UserManager"

  def init(system: ActorSystem[_]): ActorRef[UserEntity.Command] =
    ClusterSingleton(system).init(SingletonActor(apply(), NAME))

  private def apply(): Behavior[UserEntity.Command] =
    Behaviors.setup(context => new UserManager(context).eventSourcedBehavior())
}

import fusion.discoveryx.server.management.UserManager._
class UserManager(context: ActorContext[UserEntity.Command]) {
  private implicit val system = context.system
  private implicit val timeout: Timeout = 5.seconds
  private val settings = ManagementSettings(system)
  private val userEntity = UserEntity.init(system)

  def eventSourcedBehavior(): EventSourcedBehavior[UserEntity.Command, Event, UserManagerState] =
    EventSourcedBehavior[UserEntity.Command, Event, UserManagerState](
      PersistenceId.ofUniqueId(NAME),
      UserManagerState.defaultInstance,
      commandHandler,
      eventHandler).withRetention(settings.retentionCriteria)

  private def commandHandler(state: UserManagerState, command: UserEntity.Command): Effect[Event, UserManagerState] =
    command match {
      case UserManagerCommand(replyTo, cmd) => processUserManagerCommand(replyTo, state, cmd)
      case evt: Event                       => Effect.persist(evt)
      case _                                => Effect.none
    }

  private def eventHandler(state: UserManagerState, event: Event): UserManagerState = event match {
    case CreatedUserAccount(account) =>
      val accounts = if (state.accounts.contains(account)) state.accounts else account +: state.accounts
      state.copy(accounts = accounts)
    case RemovedUserAccount(account) =>
      state.copy(accounts = state.accounts.filterNot(_ == account))
  }

  private def processUserManagerCommand(
      replyTo: ActorRef[UserResponse],
      state: UserManagerState,
      command: Cmd): Effect[Event, UserManagerState] = {
    import system.executionContext
    command match {
      case Cmd.List(in) =>
        processListUser(state, in)
          .recover { case e => UserResponse(IntStatus.INTERNAL_ERROR, e.getMessage) }
          .foreach(replyTo ! _)
        Effect.none
      case Cmd.Empty => Effect.none
    }
  }

  private def processListUser(state: UserManagerState, in: ListUser)(
      implicit ec: ExecutionContext): Future[UserResponse] = {
    val (page, size, offset) = settings.generatePageSizeOffset(in.page, in.size)
    if (offset < state.accounts.size) {
      Source(state.accounts)
        .filter(account => in.account.forall(str => account.contains(str)))
        .mapAsync(math.min(8, size)) { account =>
          val cmd = UserCommand.Cmd.Query(QueryUser(in.name))
          userEntity.ask[UserResponse](queryReplyTo => ShardingEnvelope(account, UserCommand(queryReplyTo, cmd)))
        }
        .collect { case UserResponse(IntStatus.OK, _, UserResponse.Data.User(user)) => user }
        .drop(offset)
        .take(size)
        .runWith(Sink.seq)
        .map { users =>
          val data = Data.Listed(ListedUser(users, page, size, state.accounts.size))
          UserResponse(IntStatus.OK, data = data)
        }
    } else {
      Future.successful(
        UserResponse(
          IntStatus.OK,
          s"offset: $offset, but UserEntity size is ${state.accounts.size}",
          Data.Listed(ListedUser(Nil, page, size, state.accounts.size))))
    }
  }
}

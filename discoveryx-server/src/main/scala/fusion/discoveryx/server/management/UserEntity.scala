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

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import fusion.discoveryx.server.protocol.UserCommand.Cmd
import fusion.discoveryx.server.protocol.UserResponse.Data
import fusion.discoveryx.server.protocol._
import fusion.discoveryx.server.util.SessionUtils
import helloscala.common.IntStatus

object UserEntity {
  trait Command
  trait Event

  val NAME = "UserEntity"
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey(NAME)

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(TypeKey)(context => apply(context)))

  private def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    Behaviors.setup(
      context =>
        new UserEntity(PersistenceId.of(entityContext.entityTypeKey.name, entityContext.entityId), context)
          .eventSourcedBehavior())
}

import fusion.discoveryx.server.management.UserEntity._
class UserEntity private (persistenceId: PersistenceId, context: ActorContext[Command]) {
  private val settings = ManagementSettings(context.system)
  private val userManager = UserManager.init(context.system)

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, UserState] =
    EventSourcedBehavior[Command, Event, UserState](
      persistenceId,
      UserState.defaultInstance,
      commandHandler,
      eventHandler).withTagger(_ => Set(UserEntity.NAME))

  private def commandHandler(oldState: UserState, command: Command): Effect[Event, UserState] = {
    command match {
      case UserCommand(replyTo, cmd) =>
        cmd match {
          case Cmd.CheckSession(value) => processCheckSession(oldState, replyTo, value.token)
          case Cmd.Query(value)        => processQuery(oldState, replyTo, value)
          case Cmd.Create(value)       => processCreate(oldState, replyTo, value)
          case Cmd.Modify(value)       => processModify(oldState, replyTo, value)
          case Cmd.Remove(value)       => processRemove(oldState, replyTo, value)
          case Cmd.Login(value)        => processLogin(oldState, replyTo, value)
          case Cmd.Logout(_)           => processLogout(oldState, replyTo)
          case Cmd.Empty               => Effect.none
        }
      case _ => Effect.none
    }
  }

  private def processLogout(oldState: UserState, replyTo: ActorRef[UserResponse]): Effect[Event, UserState] = {
    val response = oldState.user match {
      case Some(_) => UserResponse(IntStatus.OK)
      case None    => UserResponse(IntStatus.NOT_FOUND)
    }
    replyTo ! response
    Effect.stop()
  }

  private def processLogin(
      oldState: UserState,
      replyTo: ActorRef[UserResponse],
      value: Login): Effect[Event, UserState] = {
    oldState.user match {
      case Some(_) if oldState.password == value.password =>
        Effect.persist(value).thenReply(replyTo) { state =>
          val maybe = for {
            session <- state.session
            user <- state.user
          } yield {
            UserResponse(IntStatus.OK, data = Data.Logined(Logined(session.token, user.account, user.name)))
          }
          maybe.getOrElse(UserResponse(IntStatus.INTERNAL_ERROR, "Save user session failure."))
        }
      case Some(_) =>
        Effect.reply(replyTo)(UserResponse(IntStatus.UNAUTHORIZED, "Password invalid."))
      case None =>
        replyTo ! UserResponse(IntStatus.NOT_FOUND, s"User not found, request account is ${value.account}.")
        Effect.none
    }
  }

  private def processRemove(
      oldState: UserState,
      replyTo: ActorRef[UserResponse],
      value: RemoveUser): Effect[Event, UserState] = oldState.user match {
    case Some(user) =>
      Effect.persist(value).thenStop().thenReply(replyTo) { _ =>
        userManager ! CreatedUserAccount(value.account)
        UserResponse(IntStatus.OK, data = Data.User(user))
      }
    case None =>
      Effect.stop().thenReply(replyTo)(_ => UserResponse(IntStatus.NOT_FOUND))
  }

  private def processModify(
      oldState: UserState,
      replyTo: ActorRef[UserResponse],
      value: ModifyUser): Effect[Event, UserState] = {
    oldState.user match {
      case None =>
        Effect.none.thenReply(replyTo)(_ => UserResponse(IntStatus.NOT_FOUND, "User not found."))
      case _ =>
        Effect.persist(value).thenReply(replyTo) { state =>
          UserResponse(IntStatus.OK, data = state.user.map(Data.User).getOrElse(Data.Empty))
        }
    }
  }

  private def processCreate(
      oldState: UserState,
      replyTo: ActorRef[UserResponse],
      value: CreateUser): Effect[Event, UserState] = oldState.user match {
    case Some(user) =>
      Effect.reply(replyTo)(UserResponse(IntStatus.CONFLICT, s"User exists, account is ${user.account}"))
    case _ =>
      Effect.persist[Event, UserState](value).thenReply(replyTo) {
        case UserState(Some(user), _, _) =>
          userManager ! CreatedUserAccount(user.account)
          UserResponse(IntStatus.OK, data = Data.User(user))
        case _ => UserResponse(IntStatus.INTERNAL_ERROR, "Create user failure.")
      }
  }

  private def processQuery(
      oldState: UserState,
      replyTo: ActorRef[UserResponse],
      value: QueryUser): Effect[Event, UserState] = {
    oldState match {
      case UserState(Some(user), _, _) if value.name.forall(name => user.name.contains(name)) =>
        replyTo ! UserResponse(IntStatus.OK, data = Data.User(user))
      case _ =>
        replyTo ! UserResponse(IntStatus.NOT_FOUND)
    }
    Effect.none
  }

  private def processCheckSession(
      oldState: UserState,
      replyTo: ActorRef[UserResponse],
      token: String): Effect[Event, UserState] = {
    val status =
      if (oldState.session.exists(session =>
            session.token == token && (System.currentTimeMillis() - session.activeTime) < settings.sessionTimeout))
        IntStatus.OK
      else IntStatus.UNAUTHORIZED
    Effect.persist(RefreshSession()).thenReply(replyTo)(_ => UserResponse(status))
  }

  private def eventHandler(state: UserState, event: Event): UserState = {
    event match {
      case _: RefreshSession =>
        state.copy(session = state.session.map(session => session.copy(activeTime = System.currentTimeMillis())))
      case value: CreateUser => handleCreateUser(state, value)
      case value: ModifyUser => handleModifyUser(state, value)
      case value: RemoveUser => handleRemoveUser(state, value)
      case value: Login      => handleLogin(state, value)
      case _                 => state
    }
  }

  private def handleCreateUser(state: UserState, value: CreateUser): UserState = {
    val user = state.user
      .getOrElse(User.defaultInstance)
      .copy(account = value.account, name = value.name, userRole = value.userRole)
    state.copy(user = Some(user), password = value.password)
  }

  private def handleModifyUser(state: UserState, value: ModifyUser): UserState = {
    val old = state.user.getOrElse(User.defaultInstance)
    val user = old.copy(
      name = value.name.getOrElse(old.name),
      userRole = if (value.userRole == UserRole.DEFAULT) old.userRole else value.userRole)
    state.copy(user = Some(user), password = value.password.getOrElse(state.password))
  }

  private def handleRemoveUser(state: UserState, value: RemoveUser): UserState = {
    state.copy(user = None, password = "")
  }

  private def handleLogin(state: UserState, value: Login): UserState = {
    val token = SessionUtils.generateSessionToken(value.account)
    state.copy(session = Some(UserSession(token, System.currentTimeMillis())))
  }
}

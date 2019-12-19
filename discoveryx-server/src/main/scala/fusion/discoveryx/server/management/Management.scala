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

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import fusion.discoveryx.server.protocol.ManagementCommand.Cmd
import fusion.discoveryx.server.protocol.ManagementResponse.Data
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.util.Utils

object Management {
  trait Command
  trait Event

  val NAME = "management"

  def init(system: ActorSystem[_]): ActorRef[Command] =
    ClusterSingleton(system).init(SingletonActor(apply(), Management.NAME))

  private def apply(): Behavior[Command] =
    Behaviors.setup(context => new Management(context).eventSourcedBehavior(PersistenceId.ofUniqueId(NAME)))
}

import fusion.discoveryx.server.management.Management._
class Management private (context: ActorContext[Command]) {
  private val settings = ManagementSettings(context.system)
//  private var namespaces: Vector[Namespace] = Vector()

  def eventSourcedBehavior(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ManagementState] =
    EventSourcedBehavior[Command, Event, ManagementState](
      persistenceId,
      ManagementState.defaultInstance,
      (oldState, command) => {
        command match {
          case ManagementCommand(replyTo, cmd) =>
            cmd match {
              case Cmd.List(in)   => Effect.none.thenRun(state => replyTo ! processList(in, state))
              case Cmd.Create(in) => processCreate(oldState, replyTo, in)
              case Cmd.Modify(in) => processModify(oldState, replyTo, in)
              case Cmd.Remove(in) => processRemove(oldState, replyTo, in)
              case Cmd.Empty      => Effect.reply(replyTo)(ManagementResponse(IntStatus.BAD_REQUEST, "Invalid command."))
            }
        }
      },
      eventHandler)

  private def processRemove(
      oldState: ManagementState,
      replyTo: ActorRef[ManagementResponse],
      in: RemoveNamespace): Effect[Event, ManagementState] = {
    if (oldState.namespaces.exists(_.namespace == in.namespace)) {
      Effect.persist(in).thenReply(replyTo)(_ => ManagementResponse(IntStatus.OK))
    } else {
      Effect.reply(replyTo)(ManagementResponse(IntStatus.NOT_FOUND))
    }
  }

  private def processList(in: ListNamespace, state: ManagementState): ManagementResponse = {
    val (page, size, offset) = settings.findPageSizeOffset(in.page, in.size)
    val finds = if (offset < state.namespaces.size) {
      state.namespaces.slice(offset, offset + size)
    } else {
      Nil
    }
    ManagementResponse(IntStatus.OK, data = Data.Listed(ListedNamespace(finds, page, size, state.namespaces.size)))
  }

  private def processModify(
      oldState: ManagementState,
      replyTo: ActorRef[ManagementResponse],
      in: ModifyNamespace): Effect[Event, ManagementState] = {
    if (oldState.namespaces.exists(_.namespace == in.namespace)) {
      Effect
        .persist[Event, ManagementState](in)
        .thenReply(replyTo)(
          state =>
            ManagementResponse(
              IntStatus.OK,
              data = state.namespaces.find(_.namespace == in.namespace).map(Data.Namespace).getOrElse(Data.Empty)))
    } else {
      Effect.reply(replyTo)(ManagementResponse(IntStatus.NOT_FOUND))
    }
  }

  private def processCreate(
      oldState: ManagementState,
      replyTo: ActorRef[ManagementResponse],
      in: CreateNamespace): Effect[Event, ManagementState] = {
    val either =
      if (oldState.namespaces.exists(_.name == in.name))
        Left(s"Name: ${in.name} already exists, duplicate is not allowed.")
      else Right(Namespace(Utils.timeBasedUuid().toString, in.name))

    either match {
      case Right(create) =>
        Effect
          .persist[Event, ManagementState](create)
          .thenReply(replyTo)(state =>
            ManagementResponse(
              IntStatus.OK,
              data = state.namespaces.find(_.namespace == create.namespace).map(Data.Namespace).getOrElse(Data.Empty)))

      case Left(message) =>
        Effect.reply(replyTo)(ManagementResponse(IntStatus.CONFLICT, message))
    }
  }

  private def eventHandler(state: ManagementState, event: Event): ManagementState = event match {
    case in: ModifyNamespace => eventHandleModify(in, state)
    case in: Namespace       => state.copy(namespaces = in +: state.namespaces)
    case in: RemoveNamespace =>
      val ns = state.namespaces.filterNot(_.namespace == in.namespace)
      state.copy(namespaces = ns)
  }

  private def eventHandleModify(in: ModifyNamespace, state: ManagementState): ManagementState = {
    var namespaces = state.namespaces
    val idx = namespaces.indexWhere(_.namespace == in.namespace)
    if (idx > 0) {
      val old = namespaces(idx)
      val namespace = old.copy(name = in.name.getOrElse(old.name))
      namespaces = namespaces.updated(idx, namespace)
      ManagementState(namespaces)
    } else {
      ManagementState(namespaces)
    }
  }
}

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
import akka.cluster.ddata.ORSet
import akka.cluster.ddata.typed.scaladsl.Replicator.UpdateResponse
import akka.cluster.ddata.typed.scaladsl.{ DistributedData, Replicator }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted }
import fusion.discoveryx.server.config.ConfigManager
import fusion.discoveryx.server.naming.NamingManager
import fusion.discoveryx.server.protocol.ManagementCommand.Cmd
import fusion.discoveryx.server.protocol.ManagementResponse.Data
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.util.Utils

import scala.concurrent.duration._

object Management {
  trait Command
  trait Event

  private final case class InternalUpdateResponse(resp: Replicator.UpdateResponse[ORSet[String]]) extends Command

  def init(system: ActorSystem[_]): ActorRef[Command] =
    ClusterSingleton(system).init(SingletonActor(apply(), "Management"))

  private def apply(): Behavior[Command] =
    Behaviors.setup(context => new Management(context).eventSourcedBehavior(PersistenceId("Management", "management")))
}

import fusion.discoveryx.server.management.Management._
class Management private (context: ActorContext[Command]) {
  private val settings = ManagementSettings(context.system)
  private val configManager = ConfigManager.init(context.system)
  private val namingManager = NamingManager.init(context.system)

  // 通过 DistributedData 向所有节点发送当前有校 namespace 集合
  private val distributedData = DistributedData(context.system)
  private implicit val node = distributedData.selfUniqueAddress
  private val messageAdapter: ActorRef[UpdateResponse[ORSet[String]]] =
    context.messageAdapter(resp => InternalUpdateResponse(resp))

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
              case Cmd.Get(in)    => processGet(oldState, replyTo, in)
              case Cmd.Empty      => Effect.reply(replyTo)(ManagementResponse(IntStatus.BAD_REQUEST, "Invalid command."))
            }
          case evt: ConfigSizeChangedEvent  => Effect.persist(evt)
          case evt: ServiceSizeChangedEvent => Effect.persist(evt)
          case InternalUpdateResponse(resp) =>
            context.log.debug(s"ORSet response: $resp.")
            Effect.none
        }
      },
      eventHandler)
      .receiveSignal {
        case (state, RecoveryCompleted) =>
          context.log.debug(s"RecoveryCompleted, state: $state")
          initNamespaces(state)
          storeNamespaceDistributedData(set => state.namespaces.foldLeft(set)((data, ns) => data :+ ns.namespace))
      }
      .withRetention(settings.retentionCriteria)

  private def initNamespaces(state: ManagementState): Unit = {
    for (namespace <- state.namespaces) {
      namingManager ! ShardingEnvelope(namespace.namespace, DummyNamingManager())
      configManager ! ShardingEnvelope(namespace.namespace, DummyConfigManager())
    }
  }

  private def storeNamespaceDistributedData(func: ORSet[String] => ORSet[String]): Unit = {
    distributedData.replicator ! Replicator.Update(
      NamespaceRef.Key,
      ORSet.empty[String],
      Replicator.WriteMajority(10.seconds),
      messageAdapter)(func)
  }

  private def processGet(
      oldState: ManagementState,
      replyTo: ActorRef[ManagementResponse],
      in: GetNamespace): Effect[Event, ManagementState] = {
    val resp = oldState.namespaces
      .find(_.namespace == in.namespace)
      .map(ns => ManagementResponse(IntStatus.OK, data = Data.Namespace(ns)))
      .getOrElse(ManagementResponse(IntStatus.NOT_FOUND, s"Namespace not found, namespace is ${in.namespace}"))
    Effect.reply(replyTo)(resp)
  }

  private def processRemove(
      oldState: ManagementState,
      replyTo: ActorRef[ManagementResponse],
      in: RemoveNamespace): Effect[Event, ManagementState] = {
    if (oldState.namespaces.exists(_.namespace == in.namespace)) {
      Effect.persist(in).thenReply(replyTo) { _ =>
        storeNamespaceDistributedData(set => set.remove(in.namespace))
        namingManager ! ShardingEnvelope(in.namespace, RemoveNamingManager())
        configManager ! ShardingEnvelope(in.namespace, RemoveConfigManager())
        ManagementResponse(IntStatus.OK)
      }
    } else {
      Effect.reply(replyTo)(ManagementResponse(IntStatus.NOT_FOUND))
    }
  }

  private def processList(in: ListNamespace, state: ManagementState): ManagementResponse = {
    val (page, size, offset) = settings.generatePageSizeOffset(in.page, in.size)
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
    if (oldState.namespaces.exists(_.name == in.name)) {
      Effect.reply(replyTo)(
        ManagementResponse(IntStatus.CONFLICT, s"Name: ${in.name} already exists, duplicate is not allowed."))
    } else {
      val namespace = Namespace(Utils.timeBasedUuid().toString, in.name, in.description)
      Effect.persist[Event, ManagementState](namespace).thenReply(replyTo) { _ =>
        storeNamespaceDistributedData(set => set :+ namespace.namespace)
        ManagementResponse(IntStatus.OK, data = Data.Namespace(namespace))
      }
    }
  }

  private def eventHandler(state: ManagementState, event: Event): ManagementState = event match {
    case in: ModifyNamespace         => eventHandleModify(in, state)
    case in: Namespace               => eventHandleCreate(in, state)
    case in: RemoveNamespace         => eventHandleRemove(state, in)
    case in: ConfigSizeChangedEvent  => eventHandleSize(state, in.namespace, _.copy(configCount = in.configCount))
    case in: ServiceSizeChangedEvent => eventHandleSize(state, in.namespace, _.copy(serviceCount = in.serviceCount))
  }

  private def eventHandleSize(
      state: ManagementState,
      namespace: String,
      update: Namespace => Namespace): ManagementState = {
    state.namespaces.indexWhere(_.namespace == namespace) match {
      case idx if idx < 0 => state
      case idx =>
        val namespaces = state.namespaces.updated(idx, update(state.namespaces(idx)))
        state.copy(namespaces = namespaces)
    }
  }

  private def eventHandleRemove(state: ManagementState, in: RemoveNamespace): ManagementState = {
    state.copy(namespaces = state.namespaces.filterNot(_.namespace == in.namespace))
  }

  private def eventHandleCreate(in: Namespace, state: ManagementState): ManagementState = {
    state.copy(namespaces = in +: state.namespaces)
  }

  private def eventHandleModify(in: ModifyNamespace, state: ManagementState): ManagementState = {
    val idx = state.namespaces.indexWhere(_.namespace == in.namespace)
    if (idx < 0) {
      state
    } else {
      val old = state.namespaces(idx)
      val namespace =
        old.copy(name = in.name.getOrElse(old.name), description = in.description.getOrElse(old.description))
      state.copy(namespaces = state.namespaces.updated(idx, namespace))
    }
  }
}

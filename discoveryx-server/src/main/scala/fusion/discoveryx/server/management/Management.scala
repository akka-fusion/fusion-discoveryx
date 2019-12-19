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
import akka.persistence.query.Offset
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted }
import fusion.discoveryx.model.ChangeType
import fusion.discoveryx.server.DiscoveryPersistenceQuery
import fusion.discoveryx.server.config.{ ConfigEntity, ConfigManager }
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
  private implicit val system = context.system
  private val settings = ManagementSettings(context.system)
  private val configManager: ActorRef[ShardingEnvelope[ConfigManager.Command]] = ConfigManager.init(system)
  private val readJournal = DiscoveryPersistenceQuery(system).readJournal

  // 通过 DistributedData 向所有节点发送当前有校 namespace 集合
  private val distributedData = DistributedData(system)
  implicit val node = distributedData.selfUniqueAddress
  private val messageAdapter: ActorRef[UpdateResponse[ORSet[String]]] =
    context.messageAdapter(resp => InternalUpdateResponse(resp))

  // 初始化所有ConfigManager，并让ConfigManager拥有所有自己的dataId的记录
  readJournal
    .currentPersistenceIds()
    .mapConcat { persistenceId =>
      persistenceId.split("\\" + PersistenceId.DefaultSeparator) match {
        case Array(typeName, ConfigEntity.ConfigKey(configKey)) if typeName == ConfigEntity.TypeKey.name =>
          configKey :: Nil
        case _ => Nil
      }
    }
    .groupedWithin(2048, 1.seconds)
    .filter(_.nonEmpty)
    .runForeach { list =>
      for ((namespace, keys) <- list.groupBy(_.namespace)) {
        configManager ! ShardingEnvelope(namespace, InternalConfigKeys(keys))
      }
    }

  // 监听 ConfigEntity Event 流
  readJournal
    .eventsByTag(ConfigEntity.TypeKey.name, Offset.noOffset)
    .mapConcat { envelop =>
      envelop.persistenceId.split("\\" + PersistenceId.DefaultSeparator) match {
        case Array(typeName, ConfigEntity.ConfigKey(configKey)) if typeName == ConfigEntity.TypeKey.name =>
          (configKey, envelop) :: Nil
        case _ => Nil
      }
    }
    .runForeach {
      case (configKey, envelope) =>
        envelope.event match {
          case event: ChangedConfigEvent =>
            // 向 ConfigManager 发送 ConfigKey 变化消息
            val cmd =
              if (event.`type` == ChangeType.CHANGE_REMOVE) InternalRemoveKey(configKey)
              else InternalConfigKeys(configKey :: Nil)
            configManager ! ShardingEnvelope(configKey.namespace, cmd)
          case _ => // do nothing
        }
    }

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
          case InternalUpdateResponse(resp) =>
            // TODO do nothing, logging ?
            Effect.none
        }
      },
      eventHandler).receiveSignal {
      case (state, RecoveryCompleted) =>
        storeNamespace(set => state.namespaces.foldLeft(set)((data, ns) => data :+ ns.namespace))
    }

  private def storeNamespace(func: ORSet[String] => ORSet[String]): Unit = {
    distributedData.replicator ! Replicator.Update(
      NamespaceRef.Key,
      ORSet.empty[String],
      Replicator.WriteMajority(10.seconds),
      messageAdapter)(func)
  }

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
      case Right(in) =>
        Effect.persist[Event, ManagementState](in).thenReply(replyTo) { state =>
          storeNamespace(set => set :+ in.namespace)
          ManagementResponse(
            IntStatus.OK,
            data = state.namespaces.find(_.namespace == in.namespace).map(Data.Namespace).getOrElse(Data.Empty))
        }

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
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

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.DiscoveryXUtils
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model._
import fusion.discoveryx.server.naming.{ NamingManager, NamingService, NamingSettings }
import fusion.discoveryx.server.protocol.NamingReplyCommand.Cmd
import fusion.discoveryx.server.protocol._
import fusion.discoveryx.server.util.ProtobufJson4s
import helloscala.common.IntStatus
import helloscala.common.util.StringUtils

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

private[naming] class NamingServiceBehavior(
    namespace: String,
    serviceName: String,
    context: ActorContext[NamingService.Command]) {
  import NamingService._
  private implicit val system = context.system
  private val settings = NamingSettings(context.system)
  private var unusedIdx = 0
  private var listeners: Map[ActorRef[Event], ServiceListener] = Map()

  NamingManager.init(context.system) ! ShardingEnvelope(namespace, ServiceCreatedEvent(serviceName))
  context.log.info(s"NamingService started: $namespace@$serviceName")

  def eventSourcedBehavior(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, NamingServiceState] =
    EventSourcedBehavior(persistenceId, NamingServiceState(), commandHandler, eventHandler)
      .receiveSignal {
        case (_, PostStop) =>
          cleanup()
        case (_, Terminated(ref)) =>
          try {
            listeners = listeners.removed(ref.unsafeUpcast[Event])
          } catch {
            case NonFatal(_) => // do nothing
          }
          try {
            context.self ! InstanceUnhealthyEvent(ref.unsafeUpcast[NamingInstance.Command].path.name)
          } catch {
            case NonFatal(_) => // do nothing
          }
        case (state, RecoveryCompleted) =>
          // Init NamingInstance actor
          state.instances.foreach(spawnNamingInstance)
      }
      .withTagger(_ => Set(NamingService.NAME))

  private def commandHandler(state: NamingServiceState, command: Command): Effect[Event, NamingServiceState] =
    command match {
      case NamingReplyCommand(replyTo, cmd) =>
        cmd match {
          case Cmd.Query(value)         => queryInstance(state, replyTo, value)
          case Cmd.Register(value)      => registerInstance(state, replyTo, value)
          case Cmd.Remove(value)        => removeInstance(state, replyTo, value)
          case Cmd.Modify(value)        => modifyInstance(state, replyTo, value)
          case Cmd.CreateService(value) => createService(replyTo, value)
          case Cmd.ModifyService(value) => modifyService(replyTo, value)
          case Cmd.RemoveService(value) => removeService(replyTo, value)
          case Cmd.Empty                => Effect.none
        }

      case InstancesQueried(replyTo, instances, availableTimes, query) =>
        processInstancesQueried(state, replyTo, instances, availableTimes, query)

      case Heartbeat(_, _, instId) =>
        processHeartbeat(state, instId)

      case NamingListenerCommand(ref, in) =>
        listeners = listeners.updated(ref, in)
        context.watch(ref)
        Effect.none

      case event: InstanceUnhealthyEvent =>
        notifyServiceEventListeners(NamingChangeType.INSTANCE_UNHEALTHY, NamingReply.Data.Empty)
        Effect.persist(event)

      case event: InstanceRemoveEvent =>
        notifyServiceEventListeners(NamingChangeType.INSTANCE_REMOVE, NamingReply.Data.Empty)
        Effect.persist(event)

      case evt: InstanceActorEvent =>
        if (!evt.changeType.isUnrecognized && !evt.changeType.isNamingInit) {
          notifyServiceEventListeners(evt.changeType, NamingReply.Data.Instance(evt.instance))
        }
        Effect.persist(InstanceSaveEvent(evt.instance, evt.instance.healthy))

      case StopServiceInstance() =>
        Effect.stop()

      case RemoveServiceInstance() =>
        Effect.persist(RemoveService(namespace, serviceName)).thenStop()
    }

  private def eventHandler(state: NamingServiceState, event: Event): NamingServiceState = event match {
    case InstanceSaveEvent(inst, healthy) =>
      val newState = NamingServiceStateUtils.theChanged(state, inst, healthy)
      unusedIdx = if (unusedIdx < newState.healthIds.length) unusedIdx else 0
      newState

    case InstanceRemoveEvent(instId) =>
      unusedIdx = if (unusedIdx < state.healthIds.length) unusedIdx else 0
      NamingServiceStateUtils.removeInstance(state, instId)

    case InstanceUnhealthyEvent(instId) =>
      unusedIdx = if (unusedIdx < state.healthIds.length) unusedIdx else 0
      NamingServiceStateUtils.unhealthyInstance(state, instId)

    case value: ModifyService =>
      val old = state.serviceItem
      val serviceItem = state.serviceItem.copy(
        namespace = value.namespace,
        serviceName = value.serviceName,
        groupName = value.groupName.filterNot(str => StringUtils.isBlank(str)).getOrElse(old.groupName),
        protectThreshold = value.protectThreshold.getOrElse(old.protectThreshold),
        metadata = if (value.replaceMetadata) value.metadata else old.metadata ++ value.metadata)
      state.copy(serviceItem = serviceItem)

    case value: CreateService =>
      val serviceItem = state.serviceItem.copy(
        namespace = value.namespace,
        serviceName = value.serviceName,
        groupName = if (StringUtils.isBlank(value.groupName)) Constants.DEFAULT_GROUP_NAME else value.groupName,
        protectThreshold = value.protectThreshold,
        metadata = value.metadata)
      state.copy(serviceItem = serviceItem)

    case _: RemoveService =>
      NamingServiceState.defaultInstance
  }

  private implicit val timeout: Timeout = 2.seconds
  private implicit val ec = context.executionContext

  ////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////

  private def cleanup(): Unit = {
    for ((ref, _) <- listeners) {
      ref ! ServiceEventStop()
    }
    NamingManager.init(context.system) ! ShardingEnvelope(namespace, ServiceRemovedEvent(serviceName))
  }

  private def processInstancesQueried(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      instances: Seq[Instance],
      availableTimes: Int,
      query: InstanceQuery): Effect[Event, NamingServiceState] = {
    if (instances.isEmpty && availableTimes > 0) {
      nextStateInstance(state) match {
        case Some(ref) =>
          val future =
            ref.ask[Option[Instance]](askReplyTo => NamingInstance.Get(query.oneHealthy, askReplyTo))
          context.pipeToSelf(future) {
            case Success(Some(instance)) => InstancesQueried(replyTo, instance +: instances, 0, query)
            case _                       => InstancesQueried(replyTo, instances, availableTimes - 1, query)
          }
        case _ =>
          context.self ! InstancesQueried(replyTo, instances, availableTimes - 1, query)
      }
      Effect.none
    } else {
      val resp =
        NamingReply(
          IntStatus.OK,
          data =
            NamingReply.Data.ServiceInfo(NamingServiceStateUtils.currentServiceInfo(state).copy(instances = instances)))
      Effect.reply(replyTo)(resp)
    }
  }

  private def nextStateInstance(state: NamingServiceState) = {
    if (unusedIdx < state.healthIds.length) {
      val idx = unusedIdx
      moveUnusedIdx(state)
      val instId = state.healthIds(idx)
      context.child(instId).map(_.unsafeUpcast[NamingInstance.Command])
    } else {
      None
    }
  }

  private def moveUnusedIdx(state: NamingServiceState): Unit = {
    var idx = unusedIdx + 1
    if (idx >= state.healthIds.length) {
      idx = 0
    }
    unusedIdx = idx
  }

  private def processHeartbeat(state: NamingServiceState, instanceId: String): Effect[Event, NamingServiceState] = {
    NamingServiceStateUtils.findChild(state, context, instanceId).foreach(_ ! NamingInstance.Heartbeat)
    Effect.none
  }

  private def removeService(replyTo: ActorRef[NamingReply], value: RemoveService): Effect[Event, NamingServiceState] = {
    replyTo ! NamingReply(IntStatus.OK)
    notifyServiceEventListeners(NamingChangeType.SERVICE_REMOVE, NamingReply.Data.Empty)
    Effect.persist(value).thenStop()
  }

  private def modifyService(replyTo: ActorRef[NamingReply], value: ModifyService): Effect[Event, NamingServiceState] = {
    Effect.persist(value).thenReply(replyTo) { state =>
      val data = NamingReply.Data.ServiceInfo(NamingServiceStateUtils.currentServiceInfo(state))
      notifyServiceEventListeners(NamingChangeType.SERVICE_MODIFY, data)
      NamingReply(IntStatus.OK, data = data)
    }
  }

  private def createService(replyTo: ActorRef[NamingReply], value: CreateService): Effect[Event, NamingServiceState] = {
    Effect.persist(value).thenReply(replyTo) { state =>
      val data = NamingReply.Data.ServiceInfo(NamingServiceStateUtils.currentServiceInfo(state))
      notifyServiceEventListeners(NamingChangeType.SERVICE_CREATE, data)
      NamingReply(IntStatus.OK, data = data)
    }
  }

  private def notifyServiceEventListeners(changeType: NamingChangeType, data: NamingReply.Data): Unit = {
    val event = NamingServiceEvent(
      ServiceEvent(
        changeType,
        namespace,
        serviceName,
        data.serviceInfo.map(si =>
          ServiceItem(si.namespace, si.serviceName, si.groupName, si.protectThreshold, si.metadata)),
        data.instance))
    for ((ref, _) <- listeners) {
      ref ! event
    }
  }

  private def queryInstance(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      in: InstanceQuery): Effect[Event, NamingServiceState] = {
    if (in.oneHealthy) {
      val availableTimes = state.healthIds.length
      if (availableTimes > 0) {
        context.self ! InstancesQueried(replyTo, Nil, availableTimes)
      } else {
        replyTo ! NamingReply(
          IntStatus.OK,
          data = NamingReply.Data.ServiceInfo(NamingServiceStateUtils.currentServiceInfo(state)))
      }
    } else {
      val log = context.log
      val future = Source(state.instances)
        .mapConcat { inst =>
          context.child(inst.instanceId) match {
            case Some(ref) =>
              try {
                ref.unsafeUpcast[NamingInstance.Command] :: Nil
              } catch {
                case NonFatal(e) =>
                  log.warn(s"unsafeUpcast[NamingInstance.Command] error: ${e.toString}")
                  Nil
              }
            case _ => Nil
          }
        }
        .mapAsync(4) { ref =>
          ref.ask[Option[Instance]](replyTo => NamingInstance.Get(in.allHealthy, replyTo))
        }
        .collect { case Some(instance) => instance }
        .runWith(Sink.seq)
      context.pipeToSelf(future) {
        case Success(value) => InstancesQueried(replyTo, value)
        case Failure(e) =>
          context.log.warn(s"Query instance error, parameter: $in, exception: ${e.getLocalizedMessage}")
          InstancesQueried(replyTo, Nil)
      }
    }

    Effect.none
  }

  private def modifyInstance(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      in: InstanceModify): Effect[Event, NamingServiceState] = {
    val future = NamingServiceStateUtils
      .findChild(state, context, in.instanceId)
      .map { ref =>
        ref
          .ask[Instance](replyTo => NamingInstance.Modify(in, replyTo))
          .map(instance => NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(instance)))
      }
      .getOrElse(Future.successful(NamingReply(IntStatus.NOT_FOUND)))

    future.onComplete {
      case Success(reply) =>
        if (IntStatus.isSuccess(reply.status)) {
          notifyServiceEventListeners(NamingChangeType.INSTANCE_MODIFY, reply.data)
        }
        replyTo ! reply
      case Failure(e) => replyTo ! NamingReply(IntStatus.INTERNAL_ERROR, e.getLocalizedMessage)
    }

    Effect.none
  }

  private def removeInstance(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      in: InstanceRemove): Effect[Event, NamingServiceState] = {
    val status = NamingServiceStateUtils.findChild(state, context, in.instanceId) match {
      case Some(ref) =>
        ref ! NamingInstance.Remove
        IntStatus.OK
      case _ => IntStatus.NOT_FOUND
    }
    replyTo ! NamingReply(status)
    Effect.none
  }

  private def registerInstance(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      in: InstanceRegister): Effect[Event, NamingServiceState] = {
    val response = DiscoveryXUtils.toInstance(in) match {
      case Right(inst) =>
        val maybe = NamingServiceStateUtils.findChild(state, context, inst.instanceId)
        context.log.debug(s"Child actor [NamingInstance]: $maybe. ${state.instances}")
        maybe match {
          case Some(ref) if settings.allowReplaceRegistration =>
            ref ! NamingInstance.ReplaceInstance(inst)
            NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
          case Some(_) =>
            NamingReply(IntStatus.CONFLICT, s"Service instance existed, in: ${ProtobufJson4s.toJsonString(in)}.")
          case _ =>
            spawnNamingInstance(inst)
            NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
        }

      case Left(message) =>
        NamingReply(IntStatus.BAD_REQUEST, message)
    }

    if (IntStatus.isSuccess(response.status)) {
      notifyServiceEventListeners(NamingChangeType.INSTANCE_REGISTER, response.data)
    }

    replyTo ! response
    val evt = ModifyService(
      in.namespace,
      in.serviceName,
      StringUtils.option(in.groupName).orElse(Some(Constants.DEFAULT_GROUP_NAME)),
      metadata = in.metadata)
    Effect.persist(evt)
  }

  @inline private def spawnNamingInstance(inst: Instance): Unit =
    try {
      context.spawn(
        Behaviors.supervise(NamingInstance(inst, context.self, settings)).onFailure(SupervisorStrategy.resume),
        inst.instanceId)
    } catch {
      case NonFatal(e) =>
        context.log.error(s"spawnNamingInstance($inst) error.", e)
    }
}

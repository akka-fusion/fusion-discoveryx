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

package fusion.discoveryx.server.naming.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import fusion.discoveryx.DiscoveryXUtils
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model._
import fusion.discoveryx.server.naming.{ NamingManager, NamingService, NamingSettings }
import fusion.discoveryx.server.protocol.NamingReplyCommand.Cmd
import fusion.discoveryx.server.protocol._
import helloscala.common.IntStatus
import helloscala.common.util.StringUtils

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

private[naming] class NamingServiceBehavior(
    serviceItem: ServiceItem,
    timers: TimerScheduler[NamingService.Command],
    context: ActorContext[NamingService.Command]) {
  import NamingService._
  private implicit val system = context.system
  private val settings = NamingSettings(context.system)
//  private val serviceState = new ServiceState()
  private var listeners: Map[ActorRef[Event], ServiceListener] = Map()

  NamingManager.init(context.system) ! ShardingEnvelope(serviceItem.namespace, ServiceCreated(serviceItem.serviceName))
  context.log.info(s"NamingService started: $serviceItem")

  def eventSourcedBehavior(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, NamingServiceState] =
    EventSourcedBehavior(persistenceId, NamingServiceState(), commandHandler, eventHandler).receiveSignal {
      case (_, PostStop) =>
        cleanup()
      case (_, Terminated(ref)) =>
        try {
          listeners = listeners.removed(ref.unsafeUpcast[Event])
        } catch {
          case NonFatal(_) => // do nothing
        }
        try {
          context.self ! InstanceRemoveEvent(ref.unsafeUpcast[NamingInstance.Command])
        } catch {
          case NonFatal(_) => // do nothing
        }
    }

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
          case Cmd.RemoveService(_)     => removeService(replyTo)
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

      case InternalHealthyChanged(inst, healthy) =>
        notifyServiceEventListeners(inst, healthy)

      case event: InstanceRemoveEvent =>
        Effect.persist(event)

      case evt: InstanceActorEvent =>
        Effect.persist(InstanceSaveEvent(evt.ref, evt.instance.healthy))

      case _: StopServiceInstance =>
        Effect.stop()
    }

  private def eventHandler(state: NamingServiceState, event: Event): NamingServiceState = event match {
    case InstanceSaveEvent(instanceId, healthy) =>
      state.theChanged(instanceId, healthy)

    case InstanceRemoveEvent(instanceId) =>
      val instanceIds = state.instanceIds.filterNot(_ == instanceId)
      val healthyIds = state.healthyIds.filterNot(_ == instanceId)
      val unusedIdx = if (state.unusedIdx < healthyIds.length) state.unusedIdx else 0
      state.copy(instanceIds = instanceIds, healthyIds = healthyIds, unusedIdx)

    case value: ModifyService =>
      val old = state.serviceItem
      val serviceItem = state.serviceItem.copy(
        groupName = value.groupName.filterNot(str => StringUtils.isBlank(str)).getOrElse(old.groupName),
        protectThreshold = value.protectThreshold.getOrElse(old.protectThreshold),
        metadata = if (value.replaceMetadata) value.metadata else old.metadata ++ value.metadata)
      state.copy(serviceItem = serviceItem)

    case value: CreateService =>
      val serviceItem = state.serviceItem.copy(
        groupName = if (StringUtils.isBlank(value.groupName)) Constants.DEFAULT_GROUP_NAME else value.groupName,
        protectThreshold = value.protectThreshold,
        metadata = value.metadata)
      state.copy(serviceItem = serviceItem)
  }

  private implicit val timeout: Timeout = 2.seconds
  private implicit val ec = context.executionContext

  ////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////

  private def cleanup(): Unit = {
    for ((ref, _) <- listeners) {
      ref ! ServiceEventStop()
    }
    NamingManager.init(context.system) ! ShardingEnvelope(
      serviceItem.namespace,
      ServiceRemoved(serviceItem.serviceName))
  }

  private def processInstancesQueried(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      instances: Seq[Instance],
      availableTimes: Int,
      query: InstanceQuery): Effect[Event, NamingServiceState] = {
    if (instances.isEmpty && availableTimes > 0) {
      state.nextOption() match {
        case Some(ref) =>
          val future =
            ref.ask[Option[Instance]](askReplyTo => NamingInstance.Get(query.groupName, query.oneHealthy, askReplyTo))
          context.pipeToSelf(future) {
            case Success(Some(instance)) => InstancesQueried(replyTo, instance :: Nil, 0, query)
            case _                       => InstancesQueried(replyTo, Nil, availableTimes - 1, query)
          }
        case _ =>
          context.self ! InstancesQueried(replyTo, Nil, availableTimes - 1, query)
      }
      Effect.none
    } else {
      val resp =
        NamingReply(
          IntStatus.OK,
          data = NamingReply.Data.ServiceInfo(state.currentServiceInfo().copy(instances = instances)))
      Effect.reply(replyTo)(resp)
    }
  }

  private def processHeartbeat(state: NamingServiceState, instanceId: String): Effect[Event, NamingServiceState] = {
    state.findChild(context, instanceId).foreach(_ ! NamingInstance.Heartbeat)
    Effect.none
  }

  private def removeService(replyTo: ActorRef[NamingReply]): Effect[Event, NamingServiceState] = {
    replyTo ! NamingReply(IntStatus.OK)
    notifyServiceEventListeners(NamingChangeType.SERVICE_REMOVE, NamingReply.Data.Empty)
    Effect.stop()
  }

  private def modifyService(replyTo: ActorRef[NamingReply], value: ModifyService): Effect[Event, NamingServiceState] = {
    Effect.persist(value).thenReply(replyTo) { state =>
      val data = NamingReply.Data.ServiceInfo(state.currentServiceInfo())
      notifyServiceEventListeners(NamingChangeType.SERVICE_MODIFY, data)
      NamingReply(IntStatus.OK, data = data)
    }
  }

  private def createService(replyTo: ActorRef[NamingReply], value: CreateService): Effect[Event, NamingServiceState] = {
    Effect.persist(value).thenReply(replyTo) { state =>
      val data = NamingReply.Data.ServiceInfo(state.currentServiceInfo())
      notifyServiceEventListeners(NamingChangeType.SERVICE_CREATE, data)
      NamingReply(IntStatus.OK, data = data)
    }
  }

  private def notifyServiceEventListeners(changeType: NamingChangeType, data: NamingReply.Data): Unit = {
    val event = NamingServiceEvent(
      ServiceEvent(
        changeType,
        serviceItem.namespace,
        serviceItem.serviceName,
        data.serviceInfo.map(si =>
          ServiceItem(si.namespace, si.serviceName, si.groupName, si.protectThreshold, si.metadata)),
        data.instance))
    for ((ref, _) <- listeners) {
      ref ! event
    }
  }

  private def notifyServiceEventListeners(inst: Instance, healthy: Boolean): Effect[Event, NamingServiceState] = {
    val event = NamingServiceEvent(
      ServiceEvent(
        if (healthy) NamingChangeType.INSTANCE_HEALTHY else NamingChangeType.INSTANCE_UNHEALTHY,
        serviceItem.namespace,
        serviceItem.serviceName,
        instance = Some(inst)))
    for ((ref, _) <- listeners) {
      ref ! event
    }
    Effect.none
  }

  private def queryInstance(
      state: NamingServiceState,
      replyTo: ActorRef[NamingReply],
      in: InstanceQuery): Effect[Event, NamingServiceState] = {
    if (in.oneHealthy) {
      val availableTimes = state.healthyIds.length
      if (availableTimes > 0) {
        context.self ! InstancesQueried(replyTo, Nil, availableTimes)
      } else {
        replyTo ! NamingReply(IntStatus.OK, data = NamingReply.Data.ServiceInfo(state.currentServiceInfo()))
      }
    } else {
      val future = Source(state.instanceIds)
        .mapAsync(4) { ref =>
          ref.ask[Option[Instance]](replyTo => NamingInstance.Get(in.groupName, in.allHealthy, replyTo))
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
    val future = state
      .findChild(context, in.instanceId)
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
    val status = state.findChild(context, in.instanceId) match {
      case Some(ref) =>
        ref ! NamingInstance.Remove
        notifyServiceEventListeners(NamingChangeType.INSTANCE_REMOVE, NamingReply.Data.Empty)
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
    val inst = DiscoveryXUtils.toInstance(in)
    val resp = state.findChild(context, inst.instanceId) match {
      case Some(ref) if settings.allowReplaceRegistration =>
        ref ! NamingInstance.ReplaceInstance(inst)
        NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
      case Some(_) =>
        NamingReply(IntStatus.CONFLICT, "Service instance existed.")
      case _ =>
        context.spawn(
          Behaviors.supervise(NamingInstance(inst, context.self, settings)).onFailure(SupervisorStrategy.resume),
          inst.instanceId)
        NamingReply(IntStatus.OK, data = NamingReply.Data.Instance(inst))
    }
    if (IntStatus.isSuccess(resp.status)) {
      notifyServiceEventListeners(NamingChangeType.INSTANCE_REGISTER, resp.data)
    }
    Effect.reply(replyTo)(resp)
  }
}

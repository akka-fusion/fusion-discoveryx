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

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Props }
import akka.util.Timeout
import akka.{ actor => classic }
import com.typesafe.config.Config
import fusion.common.config.FusionConfigFactory
import fusion.common.{ FusionProtocol, SpawnFactory }
import fusion.discoveryx.DiscoveryXSettings
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.namespace.NamespaceRef

import scala.concurrent.Await
import scala.concurrent.duration._

class DiscoveryX(
    val settings: DiscoveryXSettings,
    val config: Config,
    implicit val system: ActorSystem[FusionProtocol.Command])
    extends SpawnFactory {
  val namespaceRef: ActorRef[NamespaceRef.ExistNamespace] = spawn(NamespaceRef(), NamespaceRef.NAME)
  def classicSystem: classic.ActorSystem = system.toClassic

  override def spawn[T](behavior: Behavior[T], props: Props): ActorRef[T] = spawn(behavior, "", props)

  override def spawn[T](behavior: Behavior[T], name: String, props: Props): ActorRef[T] = {
    implicit val timeout: Timeout = 5.seconds
    val f = system.ask[ActorRef[T]](replyTo => FusionProtocol.Spawn(behavior, name, props, replyTo))
    Await.result(f, timeout.duration)
  }
}

object DiscoveryX {
  def fromMergedConfig(config: Config): DiscoveryX =
    fromActorSystem(ActorSystem(apply(), Constants.DISCOVERYX, config))

  def fromActorSystem(system: ActorSystem[FusionProtocol.Command]): DiscoveryX = {
    new DiscoveryX(DiscoveryXSettings(system.settings.config), system.settings.config, system)
  }

  def fromOriginalConfig(originalConfig: Config): DiscoveryX = {
    val config = FusionConfigFactory.arrangeConfig(originalConfig, Constants.DISCOVERYX, Seq("akka"))
    fromActorSystem(ActorSystem(apply(), Constants.DISCOVERYX, config))
  }

  private def apply(): Behavior[FusionProtocol.Command] = FusionProtocol.behavior
}

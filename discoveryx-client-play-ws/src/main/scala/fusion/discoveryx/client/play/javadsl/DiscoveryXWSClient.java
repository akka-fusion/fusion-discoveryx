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

package fusion.discoveryx.client.play.javadsl;

import akka.actor.typed.ActorSystem;
import akka.stream.SystemMaterializer;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.WSClient;
import play.libs.ws.ahc.AhcWSClient;
import play.libs.ws.ahc.AhcWSClientConfigFactory;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

public class DiscoveryXWSClient {
    // #standaloneWSClient
    public static DiscoveryXStandaloneWSClient standaloneWSClient(StandaloneWSClient client, ActorSystem<?> system) {
        return new DiscoveryXStandaloneWSClient(client, system);
    }

    public static DiscoveryXStandaloneWSClient standaloneWSClient(ActorSystem<?> system) {
        return standaloneWSClient(StandaloneAhcWSClient.create(
                AhcWSClientConfigFactory.forConfig(system.settings().config(), system.dynamicAccess().classLoader()),
                SystemMaterializer.get(system).materializer()), system);
    }
    // #standaloneWSClient

    // #wsClient
    public static DiscoveryXPlayWSClient wsClient(WSClient client, ActorSystem<?> system) {
        return new DiscoveryXPlayWSClient(client, system);
    }

    public static DiscoveryXPlayWSClient wsClient(AsyncHttpClient asyncHttpClient, ActorSystem<?> system) {
        return wsClient(new AhcWSClient(asyncHttpClient, SystemMaterializer.get(system).materializer()), system);
    }

    public static DiscoveryXPlayWSClient wsClient(ActorSystem<?> system) {
        return wsClient(
                AhcWSClient.create(
                        AhcWSClientConfigFactory.forConfig(system.settings().config(), system.dynamicAccess().classLoader()),
                        null,
                        SystemMaterializer.get(system).materializer()),
                system);
    }
    // #wsClient
}

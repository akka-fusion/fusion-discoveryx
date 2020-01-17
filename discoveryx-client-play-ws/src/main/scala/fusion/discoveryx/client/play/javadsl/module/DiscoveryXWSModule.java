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

package fusion.discoveryx.client.play.javadsl.module;

import akka.actor.typed.ActorSystem;
import com.typesafe.config.Config;
import fusion.discoveryx.client.play.javadsl.DiscoveryXPlay;
import fusion.discoveryx.client.play.javadsl.DiscoveryXPlayWSClient;
import fusion.discoveryx.client.play.javadsl.DiscoveryXWSClient;
import fusion.discoveryx.common.Constants;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

public class DiscoveryXWSModule extends Module {
    @Override
    public List<Binding<?>> bindings(Environment environment, Config config) {
        return Arrays.asList(bindClass(DiscoveryXPlayWSClient.class).toProvider(DiscoveryXWSPlayClientProvider.class),
                bindClass(WSClient.class).qualifiedWith(Constants.DISCOVERYX()).to(DiscoveryXPlayWSClient.class),
                bindClass(WSClient.class).qualifiedWith(DiscoveryXPlay.class).to(DiscoveryXPlayWSClient.class));
    }

    public static class DiscoveryXWSPlayClientProvider implements Provider<DiscoveryXPlayWSClient> {
        private final DiscoveryXPlayWSClient client;

        public DiscoveryXWSPlayClientProvider(AsyncHttpClient asyncHttpClient, ActorSystem<?> system) {
            this.client = DiscoveryXWSClient.wsClient(asyncHttpClient, system);
        }

        @Override
        public DiscoveryXPlayWSClient get() {
            return client;
        }
    }
}

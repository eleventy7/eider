/*
 * Copyright 2019 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.eider;

import org.junit.jupiter.api.Test;

import io.eider.common.Substrate;
import io.eider.worker.SubstrateWorker;

public class PingPongTest
{
    @Test
    public void canPingPong() throws InterruptedException
    {
        final Substrate substrate = new Substrate.SubstrateBuilder()
            .build();

        final SubstrateWorker ipcWorker1 = substrate.newWorker("ping", new DummySerializer(), new PingService());
        final SubstrateWorker ipcWorker2 = substrate.newWorker("pong", new DummySerializer(), new PongService());

        substrate.twoWayIpc(ipcWorker1, ipcWorker2, "ping-pong");

        substrate.launchOnIndividualThreads(ipcWorker1, ipcWorker2);

        Thread.sleep(10000);

        substrate.close();
    }
}

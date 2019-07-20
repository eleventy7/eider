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

package org.substrate;

import org.junit.jupiter.api.Assertions;
import org.substrate.common.Substrate;
import org.substrate.worker.SubstrateWorker;

public class PingPongTest
{

    public void canPingPong() throws InterruptedException
    {
        final Substrate substrate = new Substrate.SubstrateBuilder()
            .serializer(new DummySerializer())
            .testingMode(true)
            .build();

        final SubstrateWorker ipcWorker1 = substrate.newWorker("ping", new PingService());
        final SubstrateWorker ipcWorker2 = substrate.newWorker("pong", new PongService());

        substrate.twoWayIpc(ipcWorker1, ipcWorker2, "ping-pong");

        substrate.launchOnSharedThread(ipcWorker1, ipcWorker2);

        Thread.sleep(10000);

        Assertions.assertNotEquals(0, substrate.counters("ping-pong").messageCount);
    }
}

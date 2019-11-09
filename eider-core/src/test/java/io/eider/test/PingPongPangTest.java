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

package io.eider.test;

import org.junit.jupiter.api.Test;

import io.eider.Eider;
import io.eider.worker.Worker;

public class PingPongPangTest
{

    @Test
    public void canPingPong() throws InterruptedException
    {
        final Eider eider = new Eider.Builder()
            .enableIpc()
            .describeConfig()
            .build();

        final PingService pingService = new PingService();
        final PongService pongService = new PongService();
        final PangService pangService = new PangService();

        final Worker ipcWorker1 = eider.newWorker("ping", new DummySerializer(), pingService);
        final Worker ipcWorker2 = eider.newWorker("pong", new DummySerializer(), pongService);
        final Worker ipcWorker3 = eider.newWorker("pang", new DummySerializer(), pangService);

        //comms are pang <-> ping <-> pong (ping speaks to pong and pang, pang and pong each only speak to ping)
        eider.twoWayIpc(ipcWorker1, ipcWorker2, "ping-pong");
        eider.twoWayIpc(ipcWorker1, ipcWorker3, "ping-pang");

        eider.launchOnIndividualThreads(ipcWorker1, ipcWorker2, ipcWorker3);

        Thread.sleep(1000);

        eider.close();
    }

}

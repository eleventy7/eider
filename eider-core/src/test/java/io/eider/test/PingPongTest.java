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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.eider.common.Eider;
import io.eider.worker.Worker;

public class PingPongTest
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

        final Worker ipcWorker1 = eider.newWorker("ping", new DummySerializer(), pingService);
        final Worker ipcWorker2 = eider.newWorker("pong", new DummySerializer(), pongService);

        eider.twoWayIpc(ipcWorker1, ipcWorker2, "ping-pong");

        eider.launchOnIndividualThreads(ipcWorker1, ipcWorker2);

        Thread.sleep(500);

        int pingCount = pingService.getCount();
        int pongCount = pongService.getCount();

        System.out.println("pings received:" + pingCount);
        System.out.println("pongs received:" + pongCount);

        assertNotEquals(0, pingCount);
        assertNotEquals(0, pongCount);
        assertWithinOne(pingCount, pongCount);

        eider.close();
    }

    private void assertWithinOne(int pingCount, int pongCount)
    {
        int abs = Math.abs(pingCount - pongCount);
        assertTrue(abs >= 0 && abs <= 1);
    }
}

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
            .build();

        final SubstrateWorker ipcWorker1 = substrate.newWorker("ping", new PingService());
        final SubstrateWorker ipcWorker2 = substrate.newWorker("pong", new PongService());

        substrate.twoWayIpc(ipcWorker1, ipcWorker2, "ping-pong");

        substrate.launchOnSharedThread(ipcWorker1, ipcWorker2);

        Thread.sleep(10000);

        Assertions.assertNotEquals(0, substrate.counters("ping-pong").messageCount);
    }
}

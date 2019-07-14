package org.substrate;

import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.substrate.common.Substrate;
import org.substrate.common.SubstrateBuilder;
import org.substrate.worker.SubstrateWorker;

public class TestApi {

    public void configure() {

        final Substrate substrate = new SubstrateBuilder()
                .idleStratgy(new SleepingMillisIdleStrategy(10))
                .serializer(new DummySerializer())
                .archiverPort(5000)
                .hostAddress("127.0.0.1")
                .build();

        final SubstrateWorker ipcWorker1 = substrate.newWorker("worker1", new DummyService());
        final SubstrateWorker ipcWorker2 = substrate.newWorker("worker2", new DummyService());
        final SubstrateWorker ipcWorker3 = substrate.newWorker("worker3", new DummyService());

        substrate.twoWayIpc(ipcWorker1, ipcWorker2, "foo");
        substrate.twoWayIpc(ipcWorker2, ipcWorker3, "oof");
        substrate.externalUdpPublication(ipcWorker1, "host", 4000, 6);
        substrate.externalUdpSubscription(ipcWorker2, 4001, 5);
        substrate.externalArchivePublication(ipcWorker3, "alias");

        substrate.launchOnPrivateThread(ipcWorker1);
        substrate.launchOnSharedThread(ipcWorker2, ipcWorker3);


    }
}

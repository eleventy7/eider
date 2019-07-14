package org.substrate.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.substrate.worker.SubstrateWorker;

//holds a media driver (or archived media driver)
public class Substrate {
    private static final Logger LOG = LoggerFactory.getLogger(Substrate.class);

    public SubstrateWorker newWorker(String name, SubstrateService service) {
        return null;
    }

    public void twoWayIpc(SubstrateWorker worker1, SubstrateWorker worker2, String reference) {

    }

    public void launchOnPrivateThread(SubstrateWorker worker) {

    }

    public void externalUdpPublication(SubstrateWorker worker, String remoteHost, int port, int stream) {

    }

    public void externalUdpSubscription(SubstrateWorker worker, int port, int stream) {

    }

    public void externalArchivePublication(SubstrateWorker worker, String alias) {

    }

    public void launchOnSharedThread(SubstrateWorker ... workers) {

    }
}

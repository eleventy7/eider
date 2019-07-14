package org.substrate.worker;

import org.agrona.concurrent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubstrateWorker implements Agent {
    private static final Logger LOG = LoggerFactory.getLogger(SubstrateWorker.class);

    @Override
    public void onStart() {

    }

    @Override
    public int doWork() throws Exception {
        return 0;
    }

    @Override
    public void onClose() {

    }

    @Override
    public String roleName() {
        return null;
    }
}

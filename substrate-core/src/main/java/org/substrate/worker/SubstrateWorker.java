package org.substrate.worker;

import org.agrona.concurrent.Agent;

public class SubstrateWorker implements Agent {
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

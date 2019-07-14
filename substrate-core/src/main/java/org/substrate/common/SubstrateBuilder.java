package org.substrate.common;


import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.substrate.serialization.SubstrateSerializer;

public class SubstrateBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(SubstrateBuilder.class);

    int archiverPort = 0;
    String hostAddress = null;


    public SubstrateBuilder archiverPort(int archiverPort) {
        this.archiverPort = archiverPort;
        return this;
    }

    public SubstrateBuilder hostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
        return this;
    }

    public Substrate build() {
        return null;
    }

    public SubstrateBuilder idleStratgy(SleepingMillisIdleStrategy sleepingMillisIdleStrategy) {
        return this;
    }

    public SubstrateBuilder serializer(SubstrateSerializer serializer) {
        return this;
    }
}

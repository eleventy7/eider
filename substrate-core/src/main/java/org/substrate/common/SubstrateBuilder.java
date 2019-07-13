package org.substrate.common;


public class SubstrateBuilder {

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
}

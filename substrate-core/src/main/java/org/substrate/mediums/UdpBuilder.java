package org.substrate.mediums;

public class UdpBuilder implements MediumBuilder {
    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public boolean isArchived() {
        return false;
    }
}

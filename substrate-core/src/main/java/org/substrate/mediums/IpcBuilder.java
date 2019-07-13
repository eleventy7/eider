package org.substrate.mediums;

public class IpcBuilder implements MediumBuilder {
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public boolean isArchived() {
        return false;
    }
}

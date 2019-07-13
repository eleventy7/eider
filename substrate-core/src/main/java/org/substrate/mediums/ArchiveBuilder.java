package org.substrate.mediums;

public class ArchiveBuilder implements MediumBuilder {
    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public boolean isArchived() {
        return true;
    }
}

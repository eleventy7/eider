package org.substrate.mediums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveBuilder implements MediumBuilder
{

    private static final Logger log = LoggerFactory.getLogger(ArchiveBuilder.class);

    @Override
    public boolean isRemote()
    {
        return true;
    }

    @Override
    public boolean isArchived()
    {
        return true;
    }
}

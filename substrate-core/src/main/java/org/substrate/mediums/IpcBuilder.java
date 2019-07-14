package org.substrate.mediums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpcBuilder implements MediumBuilder
{

    private static final Logger log = LoggerFactory.getLogger(IpcBuilder.class);

    @Override
    public boolean isRemote()
    {
        return false;
    }

    @Override
    public boolean isArchived()
    {
        return false;
    }
}

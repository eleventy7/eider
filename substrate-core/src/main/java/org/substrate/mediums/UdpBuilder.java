package org.substrate.mediums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UdpBuilder implements MediumBuilder
{
    private static final Logger log = LoggerFactory.getLogger(UdpBuilder.class);

    @Override
    public boolean isRemote()
    {
        return true;
    }

    @Override
    public boolean isArchived()
    {
        return false;
    }
}

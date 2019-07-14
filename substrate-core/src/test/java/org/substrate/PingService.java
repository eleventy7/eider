package org.substrate;

import org.substrate.common.SubstrateService;

public class PingService implements SubstrateService
{
    @Override
    public void closing()
    {

    }

    @Override
    public int dutyCycle()
    {
        return 0;
    }

    @Override
    public void onMessage(final Object message, final String reference)
    {

    }


}

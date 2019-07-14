package org.substrate.serialization;

import org.agrona.DirectBuffer;
import org.substrate.common.Substrate;
import org.substrate.common.SubstrateService;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;

public class SubstrateFragmentHandler implements FragmentHandler
{
    private final SubstrateService service;
    private final Substrate substrate;

    public SubstrateFragmentHandler(final SubstrateService service, final Substrate substrate)
    {
        this.service = service;
        this.substrate = substrate;
    }

    @Override
    public void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        //get header
        //deserialize
        //call service.onMessage
    }
}

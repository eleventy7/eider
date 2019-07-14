package org.substrate;

import org.substrate.serialization.SerializationResponse;
import org.substrate.serialization.SubstrateSerializer;

public class DummySerializer implements SubstrateSerializer
{

    @Override
    public SerializationResponse serialize(final Object input)
    {
        return null;
    }

    @Override
    public Object deserialize(final byte[] input, final int messageType)
    {
        return null;
    }
}

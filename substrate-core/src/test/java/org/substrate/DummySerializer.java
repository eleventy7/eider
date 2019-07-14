package org.substrate;

import org.substrate.serialization.SubstrateSerializer;

public class DummySerializer implements SubstrateSerializer {
    @Override
    public byte[] serialize(Object input) {
        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] input) {
        return null;
    }
}

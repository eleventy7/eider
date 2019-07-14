package org.substrate.serialization;

public interface SubstrateSerializer {
    byte[] serialize(Object input);

    Object deserialize(byte[] input);
}

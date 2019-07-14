package org.substrate.serialization;

public interface SubstrateSerializer
{
    SerializationResponse serialize(Object input);

    Object deserialize(byte[] input, int messageType);
}

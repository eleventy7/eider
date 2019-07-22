package io.eider.serialization;

import static java.nio.ByteOrder.BIG_ENDIAN;

import java.nio.ByteOrder;

public final class Constants
{
    public static final int HEADER_LENGTH_OFFSET = 0;
    public static final int HEADER_OFFSET = 4;
    public static final ByteOrder DEFAULT_BYTE_ORDER = BIG_ENDIAN;

    private Constants()
    {
        //
    }
}

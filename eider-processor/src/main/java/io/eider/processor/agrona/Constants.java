package io.eider.processor.agrona;

public final class Constants
{
    private Constants()
    {
        //
    }

    static final String MUTABLE_BUFFER = "mutableBuffer";
    static final String UNSAFE_BUFFER = "unsafeBuffer";
    static final String JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN = "java.nio.ByteOrder.LITTLE_ENDIAN)";
    static final String JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1 = ", java.nio.ByteOrder.LITTLE_ENDIAN)";
    static final String TRUE = "true";
    static final String FALSE = "false";
    static final String OFFSET = "offset";
    static final String RETURN_TRUE = "return true";
    static final String RETURN_FALSE = "return false";
    static final String TRANSACTION_COPY_BUFFER_SET_FALSE = "transactionCopyBufferSet = false";
    static final String FLYWEIGHT = "_FLYWEIGHT";
    static final String WRITE = "write";
    static final String JAVA_UTIL = "java.util";
    static final String ITERATOR = "Iterator";
    static final String UNFILTERED_ITERATOR = "UnfilteredIterator";
    static final String RETURN_FLYWEIGHT = "return flyweight";
    static final String THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION =
        "throw new java.util.NoSuchElementException()";
    static final String BUFFER_LENGTH_1 = ".BUFFER_LENGTH + 1";
    static final String INTERNAL_BUFFER = "internalBuffer";
    static final String CAPACITY = "capacity";
    static final String RETURN_NULL = "return null";
    static final String BUFFER = "buffer";
    static final String FLYWEIGHT_SET_UNDERLYING_BUFFER_INTERNAL_BUFFER =
        "_FLYWEIGHT.setUnderlyingBuffer(internalBuffer, ";
}

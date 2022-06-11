/*
 * Copyright ©2019-2022 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.eider.javawriter.agrona;

public final class Constants
{
    static final String IO_EIDER_UTIL = "io.eider.util";
    static final String MUTABLE_BUFFER = "mutableBuffer";
    static final String NEW_$_T = "new $T()";
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
    static final String FIELD = " field.";
    static final String INDEX_DATA_FOR = "indexDataFor";
    static final String FLYWEIGHT_SET_UNDERLYING_BUFFER_INTERNAL_BUFFER_OFFSET =
        "flyweight.setUnderlyingBuffer(internalBuffer, offset)";
    static final String REVERSE_INDEX_DATA_FOR = "reverseIndexDataFor";

    private Constants()
    {
        //
    }
}

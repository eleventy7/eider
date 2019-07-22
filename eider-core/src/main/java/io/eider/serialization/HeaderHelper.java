/*
 * Copyright 2019 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.eider.serialization;

import java.nio.ByteBuffer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.eider.protocol.IpcHeaderDecoder;
import io.eider.protocol.IpcHeaderEncoder;
import io.eider.protocol.MessageHeaderDecoder;
import io.eider.protocol.MessageHeaderEncoder;

public class HeaderHelper
{
    private static final Logger log = LoggerFactory.getLogger(HeaderHelper.class);
    private final IpcHeaderEncoder ipcHeaderEncoder = new IpcHeaderEncoder();
    private final IpcHeaderDecoder ipcHeaderDecoder = new IpcHeaderDecoder();
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private int encode(final IpcHeaderEncoder header, final UnsafeBuffer directBuffer,
                       int messageType, String sender)
    {
        header.wrapAndApplyHeader(directBuffer, 0, headerEncoder)
            .messageType(messageType)
            .putSender(sender.getBytes(), 0, sender.length());
        return MessageHeaderEncoder.ENCODED_LENGTH + header.encodedLength();
    }

    public byte[] writeIpcHeader(String from, int messageType)
    {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        int length = encode(ipcHeaderEncoder, directBuffer, messageType, from);
        byteBuffer.limit(length);
        byte[] arr = new byte[length];
        byteBuffer.get(arr, 0, length);
        return arr;
    }

    public IpcHeaderData readIpcHeader(byte[] input)
    {
        DirectBuffer directBuffer = new UnsafeBuffer();
        directBuffer.wrap(input);

        int bufferOffset = 0;
        headerDecoder.wrap(directBuffer, bufferOffset);

        // Lookup the applicable flyweight to decode this type of message based on templateId and version.
        final int templateId = headerDecoder.templateId();
        if (templateId != IpcHeaderDecoder.TEMPLATE_ID)
        {
            throw new IllegalStateException("Template ids do not match");
        }

        final int actingBlockLength = headerDecoder.blockLength();
        final int actingVersion = headerDecoder.version();

        bufferOffset += headerDecoder.encodedLength();
        ipcHeaderDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, actingVersion);

        return new IpcHeaderData(ipcHeaderDecoder.messageType(), ipcHeaderDecoder.sender());
    }
}

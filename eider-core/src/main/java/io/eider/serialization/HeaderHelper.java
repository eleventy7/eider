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

import org.agrona.concurrent.UnsafeBuffer;

import io.eider.protocol.IpcHeaderEncoder;
import io.eider.protocol.MessageHeaderEncoder;

public class HeaderHelper
{
    final IpcHeaderEncoder header = new IpcHeaderEncoder();
    final MessageHeaderEncoder messageHeader = new MessageHeaderEncoder();

    private int encode(final IpcHeaderEncoder header, final UnsafeBuffer directBuffer,
                       final MessageHeaderEncoder messageHeaderEncoder,
                       short messageType, String sender)
    {
        header.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder);
        header.messageType(messageType);
        header.putSender(sender.getBytes(), 0, sender.length());
        return MessageHeaderEncoder.ENCODED_LENGTH + header.encodedLength();
    }

    public byte[] writeIpcHeader(String from, short messageType)
    {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        int length = encode(header, directBuffer, messageHeader, messageType, from);
        byteBuffer.limit(length);
        return byteBuffer.array();
    }
}

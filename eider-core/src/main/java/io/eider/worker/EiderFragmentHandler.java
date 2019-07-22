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

package io.eider.worker;

import static io.eider.serialization.Constants.DEFAULT_BYTE_ORDER;
import static io.eider.serialization.Constants.HEADER_LENGTH_OFFSET;
import static io.eider.serialization.Constants.HEADER_OFFSET;

import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import io.eider.serialization.EiderMessage;
import io.eider.serialization.HeaderHelper;
import io.eider.serialization.IpcHeaderData;
import io.eider.serialization.Serializer;

class EiderFragmentHandler implements FragmentHandler
{
    private static final Logger log = LoggerFactory.getLogger(EiderFragmentHandler.class);
    private final Service service;
    private final Serializer serializer;
    private final HeaderHelper headerHelper = new HeaderHelper();
    private String conduit;

    EiderFragmentHandler(final Service service, final Serializer serializer)
    {
        this.service = service;
        this.serializer = serializer;
    }

    @Override
    public void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        final int headerLen = buffer.getInt(offset + HEADER_LENGTH_OFFSET, DEFAULT_BYTE_ORDER);

        final byte[] headerRaw = new byte[headerLen];
        buffer.getBytes(offset + HEADER_OFFSET, headerRaw, 0, headerLen);
        IpcHeaderData ipcHeaderData = headerHelper.readIpcHeader(headerRaw);
        final byte[] messageContents = new byte[length - headerLen - 4];
        buffer.getBytes(4 + headerLen + offset, messageContents, 0, length - headerLen - 4);

        final EiderMessage deserialize = serializer.deserialize(messageContents, ipcHeaderData.getMessageType());

        service.onMessage(deserialize, 1, conduit, ipcHeaderData.getFrom());
    }

    void setConduit(String conduit)
    {
        this.conduit = conduit;
    }
}

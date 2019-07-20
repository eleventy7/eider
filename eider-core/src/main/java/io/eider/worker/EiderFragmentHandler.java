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

import org.agrona.DirectBuffer;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import io.eider.serialization.EiderMessage;
import io.eider.serialization.Serializer;

class EiderFragmentHandler implements FragmentHandler
{
    private final Service service;
    private final Serializer serializer;
    private String from;

    EiderFragmentHandler(final Service service, final Serializer serializer)
    {
        this.service = service;
        this.serializer = serializer;
    }

    @Override
    public void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        final EiderMessage deserialize = serializer.deserialize("ping".getBytes(), 1);
        service.onMessage(deserialize, 1, from);
    }

    void setFrom(String from)
    {
        this.from = from;
    }
}

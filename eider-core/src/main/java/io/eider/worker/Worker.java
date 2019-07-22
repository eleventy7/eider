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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.eider.common.SendStatus;
import io.eider.common.SubscriptionContainer;
import io.eider.serialization.EiderMessage;
import io.eider.serialization.HeaderHelper;
import io.eider.serialization.SerializationResponse;
import io.eider.serialization.Serializer;

public final class Worker implements Agent
{
    private static final Logger log = LoggerFactory.getLogger(Worker.class);
    private final String name;
    private final Serializer serializer;
    private final Service service;
    private final List<SubscriptionContainer> subscriptionList = new ArrayList<>();
    private final Object2ObjectHashMap<String, Object2ObjectHashMap<String, Publication>> publications =
        new Object2ObjectHashMap<>();
    private final EiderFragmentHandler handler;
    private final HeaderHelper headerHelper = new HeaderHelper();

    public Worker(String name, Serializer serializer, Service service)
    {
        this.name = name;
        this.serializer = serializer;
        this.service = service;
        this.service.setWorker(this);
        handler = new EiderFragmentHandler(service, serializer);
    }

    @Override
    public void onStart()
    {
        service.onStart();
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public int doWork()
    {
        int workCount = 0;
        for (int i = 0; i < subscriptionList.size(); i++)
        {
            SubscriptionContainer container = subscriptionList.get(i);
            handler.setConduit(container.getConduit());
            workCount += container.getSubscription().poll(handler, 10);
        }
        return workCount + service.dutyCycle();
    }

    @Override
    public void onClose()
    {
        service.onClose();
    }

    @Override
    public String roleName()
    {
        return name;
    }

    public String getName()
    {
        return name;
    }

    public void addSubscription(Subscription subscription, String conduit)
    {
        subscriptionList.add(new SubscriptionContainer(subscription, conduit));
    }

    public void addPublication(final Publication publication, final String destination, final String conduit)
    {
        if (publications.containsKey(conduit))
        {
            publications.get(conduit).put(destination, publication);
        }
        else
        {
            Object2ObjectHashMap<String, Publication> conduitMap = new Object2ObjectHashMap<>();
            conduitMap.put(destination, publication);
            publications.put(conduit, conduitMap);
        }
    }

    SendStatus send(final String conduit, final String destination, int messageType, final EiderMessage message)
    {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);

        byte[] header = headerHelper.writeIpcHeader(this.name, messageType);
        buffer.putInt(HEADER_LENGTH_OFFSET, header.length, DEFAULT_BYTE_ORDER);
        buffer.putBytes(HEADER_OFFSET, header, 0, header.length);

        SerializationResponse serialize = serializer.serialize(message);
        buffer.putBytes(4 + header.length, serialize.getData(), 0, serialize.getData().length);

        Publication publication = publications.get(conduit).get(destination);
        return SendStatus.fromOffer(publication.offer(buffer, 0, 4 + header.length + serialize.getData().length));
    }

    SendStatus reply(final int messageType, final EiderMessage message)
    {
        //
        return SendStatus.UNKNOWN;
    }

    SendStatus broadcast(final int messageType, final EiderMessage message)
    {
        return SendStatus.UNKNOWN;
    }
}

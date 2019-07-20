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

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.eider.common.SendStatus;
import io.eider.common.SubscriptionContainer;
import io.eider.common.Substrate;
import io.eider.serialization.SerializationResponse;
import io.eider.serialization.SubstrateMessage;
import io.eider.serialization.SubstrateSerializer;

public final class SubstrateWorker implements Agent
{
    private static final Logger log = LoggerFactory.getLogger(SubstrateWorker.class);
    private final String name;
    private final SubstrateSerializer serializer;
    private final SubstrateService service;
    private final List<SubscriptionContainer> subscriptionList = new ArrayList<>();
    private final Object2ObjectHashMap<String, Object2ObjectHashMap<String, Publication>> publications =
        new Object2ObjectHashMap<>();
    private final SubstrateFragmentHandler handler;
    private final Substrate substrate;

    public SubstrateWorker(String name, SubstrateSerializer serializer, SubstrateService service, Substrate substrate)
    {
        this.name = name;
        this.serializer = serializer;
        this.service = service;
        this.service.setWorker(this);
        handler = new SubstrateFragmentHandler(service, serializer, substrate);
        this.substrate = substrate;
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
            handler.setFrom(container.getName());
            log.info("polling {}: {} : {}", container.getName(), container.getSubscription().channel(),
                container.getSubscription().streamId());
            workCount += container.getSubscription().poll(handler, 10);
        }
        return workCount + service.dutyCycle();
    }

    @Override
    public void onClose()
    {
        service.closing();
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

    public SubstrateService getService()
    {
        return service;
    }

    public void addSubscription(Subscription subscription, String name)
    {
        log.info("{} has subs for conduit {} over {} and stream {}", this.getName(), name, subscription.channel(),
            subscription.streamId());
        subscriptionList.add(new SubscriptionContainer(subscription, name));
    }

    public void addPublication(final Publication publication, final String destination, final String conduit)
    {
        log.info("{} adding conduit {} to {} over {} stream {}", this.getName(), conduit, destination,
            publication.channel(),
            publication.streamId());
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

    SendStatus send(final String conduit, final String destination, final SubstrateMessage message)
    {
        Publication publication = publications.get(conduit).get(destination);

        log.info("{} sending over conduit {} to {} over {} stream {}", this.getName(), conduit, destination,
            publication.channel(), publication.streamId());

        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();
        SerializationResponse serialize = serializer.serialize(message);
        buffer.putInt(0, serialize.getType(), ByteOrder.LITTLE_ENDIAN);
        buffer.putBytes(4, serialize.getData(), 0, serialize.getData().length);

        return SendStatus.fromOffer(publication.offer(buffer, 0, 4 + serialize.getData().length));
    }

}

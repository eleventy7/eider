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

package org.substrate.worker;

import java.util.ArrayList;
import java.util.List;

import org.agrona.concurrent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.substrate.common.Substrate;
import org.substrate.common.SubstrateService;
import org.substrate.serialization.SubstrateFragmentHandler;

import io.aeron.Subscription;

public final class SubstrateWorker implements Agent
{
    private static final Logger log = LoggerFactory.getLogger(SubstrateWorker.class);
    private final String name;
    private final SubstrateService service;
    private final List<Subscription> subscriptionList = new ArrayList<>();
    private final SubstrateFragmentHandler handler;
    private final Substrate substrate;

    public SubstrateWorker(String name, SubstrateService service, Substrate substrate)
    {
        this.name = name;
        this.service = service;
        handler = new SubstrateFragmentHandler(service, substrate);
        this.substrate = substrate;
    }

    @Override
    public void onStart()
    {

    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public int doWork()
    {
        int workCount = 0;
        for (int i = 0; i < subscriptionList.size(); i++)
        {
            workCount += subscriptionList.get(i).poll(handler, 1);
        }
        return workCount + service.dutyCycle();
    }

    @Override
    public void onClose()
    {

    }

    @Override
    public String roleName()
    {
        return null;
    }

    public String getName()
    {
        return name;
    }

    public SubstrateService getService()
    {
        return service;
    }

    public void addSubscription(Subscription subscription)
    {

    }
}

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

package io.eider.common;

import java.util.ArrayList;
import java.util.List;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.eider.serialization.Serializer;
import io.eider.worker.Service;
import io.eider.worker.Worker;

public class Eider implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(Eider.class);
    private static final String IPC = "aeron:ipc";
    private final Builder builder;

    private ArchivingMediaDriver archivingMediaDriver;
    private MediaDriver mediaDriver;
    private Aeron aeron;
    private int streamId = 0;

    private List<Worker> workers;
    private List<AgentRunner> agentRunners = new ArrayList<>();

    private Eider(Builder builder)
    {
        this.builder = builder;
        log.info("Constructing Media Driver...");
        buildMediaDriver(builder);
        log.info("Constructing Aeron...");
        buildAeron(builder);
        log.info("Substrate ready...");
    }

    private void buildAeron(final Builder builder)
    {
        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName(mediaDriver.aeronDirectoryName())
            .errorHandler(this::errorHandler)
            .idleStrategy(builder.idleStrategy);
        aeron = Aeron.connect(context);
    }

    private void buildMediaDriver(final Builder builder)
    {
        MediaDriver.Context context = new MediaDriver.Context();
        context.threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .errorHandler(this::errorHandler)
            .sharedIdleStrategy(builder.idleStrategy);

        mediaDriver = MediaDriver.launchEmbedded(context);
    }

    public Worker newWorker(String name, Serializer serializer, Service service)
    {
        return new Worker(name, serializer, service, this);
    }

    public void twoWayIpc(Worker worker1, Worker worker2, String conduit)
    {
        if (!builder.enableIpc)
        {
            throw new EiderException("Cannot use IPC if enableIpc=false");
        }

        int oneToTwoStreamId = nextStreamId();
        int twoToOneStreamId = nextStreamId();

        Publication oneToTwo = aeron.addPublication(IPC, oneToTwoStreamId);
        Publication twoToOne = aeron.addPublication(IPC, twoToOneStreamId);
        worker1.addPublication(oneToTwo, worker2.getName(), conduit);
        worker2.addPublication(twoToOne, worker1.getName(), conduit);

        Subscription oneToTwoSubs = aeron.addSubscription(IPC, oneToTwoStreamId);
        Subscription twoToOneSubs = aeron.addSubscription(IPC, twoToOneStreamId);
        worker1.addSubscription(twoToOneSubs, conduit);
        worker2.addSubscription(oneToTwoSubs, conduit);

        describe(oneToTwo, conduit, worker1);
        describe(oneToTwoSubs, conduit, worker2);
        describe(twoToOneSubs, conduit, worker1);
        describe(twoToOne, conduit, worker2);
    }

    private void ipcListener(Worker worker, String reference)
    {

    }

    private void ipcWriter(Worker worker, String reference)
    {

    }

    private void describe(Subscription subscription, String conduit, Worker worker)
    {
        if (builder.describeConfig)
        {
            log.info("New listerner for worker [{}] on conduit [{}] over [{}] and stream [{}]", worker.getName(),
                conduit, subscription.channel(), subscription.streamId());
        }
    }

    private void describe(Publication publication, String conduit, Worker worker)
    {
        if (builder.describeConfig)
        {
            log.info("Send destination added for worker [{}] on conduit [{}] over [{}] and stream [{}]",
                worker.getName(),
                conduit, publication.channel(), publication.streamId());
        }
    }

    public void launchOnThread(Worker worker)
    {
        AgentRunner runner = new AgentRunner(builder.idleStrategy, this::errorHandler, null, worker);
        agentRunners.add(runner);
        AgentRunner.startOnThread(runner);
    }

    private void errorHandler(final Throwable throwable)
    {
        log.error(throwable.getMessage(), throwable);
    }

    public void udpWriter(Worker worker, String reference, String remoteHost, int port, int stream)
    {

    }

    public void udpListener(Worker worker, String reference, int port, int stream)
    {

    }

    public void archiveWriter(Worker worker, String reference, String alias)
    {

    }

    public void archiveReader(Worker worker, String reference, String alias)
    {

    }

    public void launchOnSharedThread(Worker... workers)
    {
        CompositeAgent agent = new CompositeAgent(workers);
        AgentRunner runner = new AgentRunner(builder.idleStrategy, this::errorHandler, null, agent);
        agentRunners.add(runner);
        AgentRunner.startOnThread(runner);

    }

    public void launchOnIndividualThreads(Worker... workers)
    {
        for (Worker worker : workers)
        {
            launchOnThread(worker);
        }
    }

    @Override
    public void close()
    {
        agentRunners.forEach(AgentRunner::close);

        if (archivingMediaDriver != null)
        {
            archivingMediaDriver.close();
        }

        if (mediaDriver != null)
        {
            mediaDriver.close();
        }

        if (aeron != null)
        {
            aeron.close();
        }
    }

    public SubstrateCounters counters(final String conduit)
    {
        return null;
    }

    private int nextStreamId()
    {
        return ++streamId;
    }

    public static class Builder
    {
        int archiverPort = 0;
        String hostAddress = null;
        boolean requiresArchivingMediaDriver = false;
        boolean testingMode = false;
        IdleStrategy idleStrategy = new SleepingMillisIdleStrategy(10);
        boolean enableIpc = false;
        boolean describeConfig = false;

        public Builder archiverPort(int archiverPort)
        {
            requiresArchivingMediaDriver = true;
            this.archiverPort = archiverPort;
            return this;
        }

        public Builder hostAddress(String hostAddress)
        {
            this.hostAddress = hostAddress;
            return this;
        }


        public Builder testingMode(boolean testingMode)
        {
            this.testingMode = testingMode;
            return this;
        }

        public Eider build()
        {
            return new Eider(this);
        }

        public Builder idleStratgy(IdleStrategy idleStrategy)
        {
            this.idleStrategy = idleStrategy;
            return this;
        }

        public Builder enableIpc()
        {
            enableIpc = true;
            return this;
        }

        public Builder describeConfig()
        {
            describeConfig = true;
            return this;
        }
    }
}
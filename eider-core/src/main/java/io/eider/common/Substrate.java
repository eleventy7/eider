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
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import io.eider.serialization.SubstrateSerializer;
import io.eider.worker.SubstrateService;
import io.eider.worker.SubstrateWorker;

public class Substrate implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(Substrate.class);

    private ArchivingMediaDriver archivingMediaDriver;
    private MediaDriver mediaDriver;
    private Aeron aeron;
    private IdleStrategy idleStrategy;

    private List<SubstrateWorker> workers;
    private List<AgentRunner> agentRunners = new ArrayList<>();

    private Substrate(SubstrateBuilder builder)
    {
        //
        log.info("Constructing Media Driver...");
        buildMediaDriver(builder);
        log.info("Constructing Aeron...");
        buildAeron(builder);
    }

    private void buildAeron(final SubstrateBuilder builder)
    {
        //fop
    }

    private void buildMediaDriver(final SubstrateBuilder builder)
    {
        //foo
    }

    public SubstrateWorker newWorker(String name, SubstrateService service)
    {
        return null;
    }

    public void twoWayIpc(SubstrateWorker worker1, SubstrateWorker worker2, String conduit)
    {

    }

    private void ipcListener(SubstrateWorker worker, String reference)
    {

    }

    private void ipcWriter(SubstrateWorker worker, String reference)
    {

    }

    public void launchOnThread(SubstrateWorker worker)
    {
        AgentRunner runner = new AgentRunner(this.idleStrategy, this::errorHandler, null, worker);
        agentRunners.add(runner);
        AgentRunner.startOnThread(runner);
    }

    private void errorHandler(final Throwable throwable)
    {

    }

    public void udpWriter(SubstrateWorker worker, String reference, String remoteHost, int port, int stream)
    {

    }

    public void udpListener(SubstrateWorker worker, String reference, int port, int stream)
    {

    }

    public void archiveWriter(SubstrateWorker worker, String reference, String alias)
    {

    }

    public void archiveReader(SubstrateWorker worker, String reference, String alias)
    {

    }

    public void launchOnSharedThread(SubstrateWorker... workers)
    {
        CompositeAgent agent = new CompositeAgent(workers);
        AgentRunner runner = new AgentRunner(this.idleStrategy, this::errorHandler, null, agent);
        agentRunners.add(runner);
        AgentRunner.startOnThread(runner);

    }

    public void launchOnIndividualThreads(SubstrateWorker... workers)
    {
        for (SubstrateWorker worker : workers)
        {
            launchOnThread(worker);
        }
    }

    @Override
    public void close()
    {
        workers.forEach(worker -> worker.getService().closing());

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

    public static class SubstrateBuilder
    {
        int archiverPort = 0;
        String hostAddress = null;
        boolean requiresArchivingMediaDriver = false;
        boolean testingMode = false;


        public SubstrateBuilder archiverPort(int archiverPort)
        {
            requiresArchivingMediaDriver = true;
            this.archiverPort = archiverPort;
            return this;
        }

        public SubstrateBuilder hostAddress(String hostAddress)
        {
            this.hostAddress = hostAddress;
            return this;
        }


        public SubstrateBuilder testingMode(boolean testingMode)
        {
            this.testingMode = testingMode;
            return this;
        }

        public Substrate build()
        {
            log.info("foo!");
            return null;
        }

        public SubstrateBuilder idleStratgy(SleepingMillisIdleStrategy sleepingMillisIdleStrategy)
        {
            return this;
        }

        public SubstrateBuilder serializer(SubstrateSerializer serializer)
        {
            return this;
        }
    }
}
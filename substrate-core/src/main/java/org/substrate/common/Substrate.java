package org.substrate.common;

import java.util.ArrayList;
import java.util.List;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.substrate.serialization.SubstrateSerializer;
import org.substrate.worker.SubstrateWorker;

import io.aeron.Aeron;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;

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

    public void twoWayIpc(SubstrateWorker worker1, SubstrateWorker worker2, String reference)
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

        if (archivingMediaDriver != null) {
            archivingMediaDriver.close();
        }

        if (mediaDriver != null) {
            mediaDriver.close();
        }

        if (aeron != null) {
            aeron.close();
        }
    }

    public SubstrateCounters counters(final String reference)
    {
        return null;
    }

    public static class SubstrateBuilder
    {
        int archiverPort = 0;
        String hostAddress = null;
        boolean requiresArchivingMediaDriver = false;


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

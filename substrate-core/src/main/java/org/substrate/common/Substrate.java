package org.substrate.common;

import java.util.List;

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

    private List<SubstrateWorker> workers;

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

    public void launchOnPrivateThread(SubstrateWorker worker)
    {

    }

    public void externalUdpPublication(SubstrateWorker worker, String remoteHost, int port, int stream)
    {

    }

    public void externalUdpSubscription(SubstrateWorker worker, int port, int stream)
    {

    }

    public void externalArchivePublication(SubstrateWorker worker, String alias)
    {

    }

    public void launchOnSharedThread(SubstrateWorker... workers)
    {

    }

    public void launchOnIndividualThreads(SubstrateWorker... workers)
    {

    }

    @Override
    public void close() throws Exception
    {
        workers.forEach(worker -> worker.getService().closing());
        workers.forEach(SubstrateWorker::onClose);

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

package io.eider.performance;

import com.foo.sample.gen.EiderObject;
import com.foo.sample.gen.LongObject;

import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;

public class SpecPerformanceTests
{
    public static UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(50));

    @Benchmark
    public void writeStringLongBoolean(Blackhole bh)
    {
        EiderObject eiderObject = new EiderObject();
        eiderObject.setBufferWriteHeader(buffer, 0);
        eiderObject.writeCusip("CUSIP0001");
        eiderObject.writeEnabled(false);
        eiderObject.writeTimestamp(0L);

        bh.consume(eiderObject.readEnabled());
        bh.consume(eiderObject.readCusip());
        bh.consume(eiderObject.readTimestamp());
    }

    @Benchmark
    public void writeLong(Blackhole bh)
    {
        LongObject longObject = new LongObject();
        longObject.setUnderlyingBuffer(buffer, 0);
        longObject.writeTimestamp1(1L);
        longObject.writeTimestamp2(1L);

        bh.consume(longObject.readTimestamp1());
        bh.consume(longObject.readTimestamp2());
    }
}

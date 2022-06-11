/*
 * Copyright ©2019-2022 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */

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

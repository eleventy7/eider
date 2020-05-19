/*
 * Copyright 2019-2020 Shaun Laurens.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foo.sample;

import com.foo.sample.gen.EiderObjectA;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.SystemEpochClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SampleImplementationTest
{
    @Test
    public void canSerialize()
    {
        final EiderObjectA eiderR = new EiderObjectA();
        final EiderObjectA eiderW = new EiderObjectA();
        final EpochClock clock = new SystemEpochClock();
        final long now = clock.time();

        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(EiderObjectA.BUFFER_LENGTH);

        eiderW.setWriteBuffer(buffer, 0);
        eiderW.writeHeader();
        eiderW.writeCusip("037833100");
        eiderW.writeEnabled(true);
        eiderW.writeId(213);
        eiderW.writeTimestamp(now);

        eiderR.setReadBuffer(buffer, 0);
        Assertions.assertTrue(eiderR.validateHeader());
        Assertions.assertEquals("037833100", eiderR.readCusip());
        Assertions.assertTrue(eiderR.readEnabled());
        Assertions.assertEquals(now, eiderR.readTimestamp());
        Assertions.assertEquals(213, eiderR.readId());
    }
}

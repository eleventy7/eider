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

import com.foo.sample.gen.EiderObject;
import com.foo.sample.gen.EiderObjectRepository;
import com.foo.sample.gen.SequenceGenerator;

import io.eider.Helper.EiderHelper;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.SystemEpochClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SampleImplementationTest
{

    public static final String CUSIP = "037833100";

    @Test
    public void canReadWrite()
    {
        final EiderObject eiderR = new EiderObject();
        final EiderObject eiderW = new EiderObject();
        final EpochClock clock = new SystemEpochClock();
        final long now = clock.time();

        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(EiderObject.BUFFER_LENGTH);

        eiderW.setUnderlyingBuffer(buffer, 0);
        eiderR.setUnderlyingBuffer(buffer, 0);

        eiderW.writeHeader();
        eiderW.writeCusip(CUSIP);
        eiderW.writeEnabled(true);
        eiderW.writeId(213);
        eiderW.writeTimestamp(now);


        Assertions.assertTrue(eiderR.validateHeader());
        assertEquals(CUSIP, eiderR.readCusip());
        Assertions.assertTrue(eiderR.readEnabled());
        assertEquals(now, eiderR.readTimestamp());
        assertEquals(213, eiderR.readId());

        assertEquals(EiderObject.EIDER_ID, EiderHelper.getEiderId(buffer, 0));
        assertEquals(EiderObject.EIDER_GROUP_ID, EiderHelper.getEiderGroupId(buffer, 0));
    }

    @Test
    public void canRollback()
    {
        final EiderObject eiderR = new EiderObject();
        final EiderObject eiderW = new EiderObject();
        final EpochClock clock = new SystemEpochClock();
        final long now = clock.time();

        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(EiderObject.BUFFER_LENGTH);

        eiderR.setUnderlyingBuffer(buffer, 0);
        eiderW.setUnderlyingBuffer(buffer, 0);

        eiderW.writeHeader();
        eiderW.writeCusip(CUSIP);
        eiderW.writeEnabled(true);
        eiderW.writeId(213);
        eiderW.writeTimestamp(now);

        assertEquals(CUSIP, eiderR.readCusip());

        eiderW.beginTransaction();
        eiderW.writeCusip("zzzzzzzzz");
        //by default dirty reads are supported
        assertEquals("zzzzzzzzz", eiderR.readCusip());
        eiderW.rollback();

        assertEquals(CUSIP, eiderR.readCusip());
    }

    @Test
    public void sequencesWork()
    {
        final SequenceGenerator generator = new SequenceGenerator();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SequenceGenerator.BUFFER_LENGTH);
        generator.setUnderlyingBuffer(buffer, 0);

        generator.initializeOrderId(1);

        final int nextOrderIdSequence = generator.nextOrderIdSequence();

        assertEquals(2, nextOrderIdSequence);
    }

    @Test
    public void canUseRepository()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = new SequenceGenerator();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SequenceGenerator.BUFFER_LENGTH);
        generator.setUnderlyingBuffer(buffer, 0);

        EiderObject flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);
        flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        EiderObject flyRead = repository.getByKey(1);
        assert flyRead != null;
        assertEquals("CUSIP0001", flyRead.readCusip());
        flyRead = repository.getByKey(2);
        assert flyRead != null;
        assertEquals("CUSIP0002", flyRead.readCusip());

        flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assertNull(flyWrite);
        assertEquals(294084336, repository.getCrc32());
    }

    @Test
    public void canUseTransactionalRepository()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = new SequenceGenerator();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SequenceGenerator.BUFFER_LENGTH);
        generator.setUnderlyingBuffer(buffer, 0);

        EiderObject flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);

        long initalCrc32 = repository.getCrc32();

        repository.beginTransaction();

        flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        EiderObject flyRead = repository.getByKey(1);
        assert flyRead != null;
        assertEquals("CUSIP0001", flyRead.readCusip());

        flyRead = repository.getByKey(2);
        assert flyRead != null;
        assertEquals("CUSIP0002", flyRead.readCusip());

        flyWrite = repository.getByKey(1);
        flyWrite.writeCusip("ABCDEFGHI");
        flyRead = repository.getByKey(1);
        assertEquals("ABCDEFGHI", flyRead.readCusip());

        repository.rollback();

        flyRead = repository.getByKey(1);
        assertEquals("CUSIP0001", flyRead.readCusip());

        EiderObject nullExpected = repository.getByKey(2);
        assertNull(nullExpected);

        assertEquals(initalCrc32, repository.getCrc32());
    }

    @Test
    public void canDetectChangesWithCrc32ChangedData()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(1);

        final SequenceGenerator generator = new SequenceGenerator();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SequenceGenerator.BUFFER_LENGTH);
        generator.setUnderlyingBuffer(buffer, 0);

        EiderObject flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);

        long initialCrc = repository.getCrc32();

        flyWrite.writeCusip("CUSIP0003");

        assertNotEquals(initialCrc, repository.getCrc32());
    }

    @Test
    public void crc32EqualSameContentsDifferentBuffers()
    {
        final EiderObjectRepository repositoryA = EiderObjectRepository.createWithCapacity(1);
        final EiderObjectRepository repositoryB = EiderObjectRepository.createWithCapacity(1);

        EiderObject flyWriteA = repositoryA.appendWithKey(1);
        assert flyWriteA != null;
        flyWriteA.writeCusip("CUSIP0001");
        flyWriteA.writeEnabled(true);
        flyWriteA.writeTimestamp(0);

        long crcA = repositoryA.getCrc32();

        EiderObject flyWriteB = repositoryB.appendWithKey(1);
        assert flyWriteB != null;
        flyWriteB.writeCusip("CUSIP0001");
        flyWriteB.writeEnabled(true);
        flyWriteB.writeTimestamp(0);

        long crcB = repositoryB.getCrc32();

        assertEquals(crcA, crcB);
    }

    @Test
    public void canDetectChangesWithCrc32NewElements()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = new SequenceGenerator();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SequenceGenerator.BUFFER_LENGTH);
        generator.setUnderlyingBuffer(buffer, 0);

        EiderObject flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);

        long initialCrc = repository.getCrc32();

        flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);

        assertNotEquals(initialCrc, repository.getCrc32());
    }
}

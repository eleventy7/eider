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

package com.foo.sample;

import com.foo.sample.gen.EiderObject;
import com.foo.sample.gen.EiderObjectRepository;
import com.foo.sample.gen.SequenceGenerator;

import io.eider.util.EiderHelper;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.SystemEpochClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleImplementationTest
{

    public static final String CUSIP = "037833100";

    @Test
    void canReadWrite()
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
    void canReadWriteStringDiffLength()
    {
        final EiderObject eiderR = new EiderObject();
        final EiderObject eiderW = new EiderObject();

        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(EiderObject.BUFFER_LENGTH);

        eiderW.setUnderlyingBuffer(buffer, 0);
        eiderR.setUnderlyingBuffer(buffer, 0);

        eiderW.writeCusip("012345678");
        assertEquals("012345678", eiderR.readCusip());

        eiderW.writeCusipWithPadding("ABC");
        assertEquals("ABC", eiderR.readCusip());

        eiderW.writeCusip("012345678");
        assertEquals("012345678", eiderR.readCusip());

        eiderW.writeCusip("DEF");
        assertEquals("DEF345678", eiderR.readCusip());
    }

    @Test
    void canRollback()
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
    void sequencesWork()
    {
        final SequenceGenerator generator =  SequenceGenerator.INSTANCE();

        assertEquals(1, generator.nextOrderIdSequence());
        assertEquals(2, generator.nextOrderIdSequence());
        assertEquals(3, generator.nextOrderIdSequence());
    }

    @Test
    void canUseRepository()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = SequenceGenerator.INSTANCE();

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

        //when can no longer append (exceeds capacity), should return null
        flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assertNull(flyWrite);
        assertEquals(201450551L, repository.getCrc32());
    }

    @Test
    void canUseRepositoryByIndexBoolean()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(3);
        EiderObject flyWrite = repository.appendWithKey(90);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);
        flyWrite = repository.appendWithKey(91);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);
        flyWrite = repository.appendWithKey(92);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0003");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        List<Integer> allEnabledEqualTrue = repository.getAllWithIndexEnabledValue(true);
        List<Integer> allEnabledEqualFalse = repository.getAllWithIndexEnabledValue(false);

        assertEquals(1, allEnabledEqualTrue.size());
        assertEquals(2, allEnabledEqualFalse.size());

        EiderObject flyRead;

        flyRead = repository.getByBufferOffset(allEnabledEqualTrue.get(0));
        assertNotNull(flyRead);
        assertTrue(flyRead.readEnabled());
        assertEquals("CUSIP0001", flyRead.readCusip());

        flyRead = repository.getByBufferOffset(allEnabledEqualFalse.get(0));
        assertNotNull(flyRead);
        assertFalse(flyRead.readEnabled());
        assertEquals("CUSIP0003", flyRead.readCusip());

        flyRead = repository.getByBufferOffset(allEnabledEqualFalse.get(1));
        assertNotNull(flyRead);
        assertFalse(flyRead.readEnabled());
        assertEquals("CUSIP0002", flyRead.readCusip());

    }

    @Test
    void canUseRepositoryByIndexString()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(3);
        EiderObject flyWrite = repository.appendWithKey(90);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);
        flyWrite = repository.appendWithKey(91);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);
        flyWrite = repository.appendWithKey(92);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0003");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        List<Integer> allCusip1 = repository.getAllWithIndexCusipValue("CUSIP0001");
        List<Integer> allCusip3 = repository.getAllWithIndexCusipValue("CUSIP0003");

        assertEquals(1, allCusip1.size());
        assertEquals(1, allCusip3.size());

        EiderObject flyRead;

        flyRead = repository.getByBufferOffset(allCusip1.get(0));
        assertNotNull(flyRead);
        assertTrue(flyRead.readEnabled());
        assertEquals("CUSIP0001", flyRead.readCusip());

        flyRead = repository.getByBufferOffset(allCusip3.get(0));
        assertNotNull(flyRead);
        assertFalse(flyRead.readEnabled());
        assertEquals("CUSIP0003", flyRead.readCusip());
    }

    @Test
    void canUseRepositoryForSnapshotWriteRead()
    {
        final EiderObjectRepository sourceRepo = EiderObjectRepository.createWithCapacity(3);
        EiderObject flyWrite = sourceRepo.appendWithKey(90);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);
        flyWrite = sourceRepo.appendWithKey(91);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);
        flyWrite = sourceRepo.appendWithKey(92);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0003");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        final EiderObjectRepository destRepo = EiderObjectRepository.createWithCapacity(3);

        for (int i = 0; i < sourceRepo.getCurrentCount(); i++)
        {
            int offset = sourceRepo.getOffsetByBufferIndex(i);
            EiderObject eiderObject = destRepo.appendByCopyFromBuffer(sourceRepo.getUnderlyingBuffer(), offset);
            assertNotNull(eiderObject);
        }

        EiderObject validator = destRepo.getByBufferIndex(0);
        assertEquals("CUSIP0001", validator.readCusip());
        assertEquals(90, validator.readId());
        assertEquals(true, validator.readEnabled());
        assertEquals(0, validator.readTimestamp());

        validator = destRepo.getByBufferIndex(1);
        assertEquals("CUSIP0002", validator.readCusip());
        assertEquals(91, validator.readId());
        assertEquals(false, validator.readEnabled());
        assertEquals(1, validator.readTimestamp());

        validator = destRepo.getByBufferIndex(2);
        assertEquals("CUSIP0003", validator.readCusip());
        assertEquals(92, validator.readId());
        assertEquals(false, validator.readEnabled());
        assertEquals(1, validator.readTimestamp());

        assertEquals(sourceRepo.getCrc32(), destRepo.getCrc32());
        assertEquals(sourceRepo.getAllWithIndexCusipValue("CUSIP0001").size(),
            destRepo.getAllWithIndexCusipValue("CUSIP0001").size());

        assertEquals(sourceRepo.getAllWithIndexCusipValue("CUSIP0001").get(0),
            destRepo.getAllWithIndexCusipValue("CUSIP0001").get(0));
    }

    @Test
    void canHandleIndexedDataUpdates()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(3);
        EiderObject flyWrite = repository.appendWithKey(90);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);
        flyWrite = repository.appendWithKey(91);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);
        flyWrite = repository.appendWithKey(92);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0003");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        List<Integer> allCusip1 = repository.getAllWithIndexCusipValue("CUSIP0001");
        List<Integer> allCusip3 = repository.getAllWithIndexCusipValue("CUSIP0003");

        assertEquals(1, allCusip1.size());
        assertEquals(1, allCusip3.size());

        EiderObject flyMutableRead;

        flyMutableRead = repository.getByBufferOffset(allCusip1.get(0));
        assertNotNull(flyMutableRead);
        flyMutableRead.writeCusip("CUSIP0004");

        List<Integer> allCusip1NowEmpty = repository.getAllWithIndexCusipValue("CUSIP0001");
        List<Integer> allCusip4 = repository.getAllWithIndexCusipValue("CUSIP0003");

        assertEquals(0, allCusip1NowEmpty.size());
        assertEquals(1, allCusip4.size());
    }

    @Test
    void canUseRepositoryByOffset()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(3);

        EiderObject flyWrite = repository.appendWithKey(90);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0001");
        flyWrite.writeEnabled(true);
        flyWrite.writeTimestamp(0);
        flyWrite = repository.appendWithKey(91);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0002");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);
        flyWrite = repository.appendWithKey(92);
        assert flyWrite != null;
        flyWrite.writeCusip("CUSIP0003");
        flyWrite.writeEnabled(false);
        flyWrite.writeTimestamp(1);

        EiderObject flyRead = repository.getByKey(90);
        assert flyRead != null;
        assertEquals("CUSIP0001", flyRead.readCusip());

        flyRead = repository.getByBufferOffset(0);
        assert flyRead != null;
        assertEquals("CUSIP0001", flyRead.readCusip());

        flyRead = repository.getByKey(91);
        assert flyRead != null;
        assertEquals("CUSIP0002", flyRead.readCusip());

        flyRead = repository.getByBufferOffset(EiderObject.BUFFER_LENGTH + 1);
        assert flyRead != null;
        assertEquals("CUSIP0002", flyRead.readCusip());

        flyRead = repository.getByKey(92);
        assert flyRead != null;
        assertEquals("CUSIP0003", flyRead.readCusip());

        flyRead = repository.getByBufferOffset((EiderObject.BUFFER_LENGTH * 2) + 2);
        assert flyRead != null;
        assertEquals("CUSIP0003", flyRead.readCusip());

        flyWrite = repository.getByBufferOffset(3);
        assertNull(flyWrite);
        assertEquals(2255832961L, repository.getCrc32());
    }

    @Test
    void canUseTransactionalRepository()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = SequenceGenerator.INSTANCE();

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
        assertNotNull(flyWrite);
        flyWrite.writeCusip("ABCDEFGHI");
        flyRead = repository.getByKey(1);
        assertNotNull(flyRead);
        assertEquals("ABCDEFGHI", flyRead.readCusip());

        List<Integer> preCommitItem = repository.getAllWithIndexCusipValue("ABCDEFGHI");
        assertEquals(1, preCommitItem.size());

        repository.rollback();

        flyRead = repository.getByKey(1);
        assertNotNull(flyRead);
        assertEquals("CUSIP0001", flyRead.readCusip());

        EiderObject nullExpected = repository.getByKey(2);
        assertNull(nullExpected);

        List<Integer> postRollbackItem = repository.getAllWithIndexCusipValue("ABCDEFGHI");
        assertEquals(0, postRollbackItem.size());

        assertEquals(initalCrc32, repository.getCrc32());
    }

    @Test
    void canDetectChangesWithCrc32ChangedData()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(1);

        final SequenceGenerator generator = SequenceGenerator.INSTANCE();

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
    void crc32EqualSameContentsDifferentBuffers()
    {
        final EiderObjectRepository repositoryA = EiderObjectRepository.createWithCapacity(100_000);
        final EiderObjectRepository repositoryB = EiderObjectRepository.createWithCapacity(100_000);

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
    void canDetectChangesWithCrc32NewElements()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = SequenceGenerator.INSTANCE();

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


    @Test
    void canUseUniqueRepository()
    {
        final EiderObjectRepository repository = EiderObjectRepository.createWithCapacity(2);

        final SequenceGenerator generator = SequenceGenerator.INSTANCE();

        EiderObject flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        boolean allowedWrite = flyWrite.writeCusip("CUSIP0001");
        assertTrue(allowedWrite);

        flyWrite = repository.appendWithKey(generator.nextOrderIdSequence());
        assert flyWrite != null;
        boolean allowedWriteAgain = flyWrite.writeCusip("CUSIP0001");
        assertFalse(allowedWriteAgain);
    }
}

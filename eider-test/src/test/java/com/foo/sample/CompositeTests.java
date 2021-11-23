package com.foo.sample;

import com.foo.sample.gen.Order;
import com.foo.sample.gen.OrderBook;
import com.foo.sample.gen.OrderBookEntry;

import com.foo.sample.gen.OrderBookEntryRepository;

import io.eider.util.EiderHelper;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeTests
{
    public static final String INSTRUMENT = "012345678";
    public static final String ORDER_123 = "ORDER123";

    @Test
    void canReadWriteCompositeInternalBuffer()
    {
        OrderBookEntry entry = new OrderBookEntry();
        entry.writeId(45);
        entry.getOrder().writeClOrdId(ORDER_123);
        entry.getOrder().writePrice(12300L);
        entry.getOrder().writeQuantity(1_000_000L);
        entry.getStatus().writeAcceptedTimestamp(500L);
        entry.getStatus().writeFilledTimestamp(800L);

        assertEquals(45, entry.readId());
        assertEquals(ORDER_123, entry.getOrder().readClOrdId());
        assertEquals(12300L, entry.getOrder().readPrice());
        assertEquals(1_000_000L, entry.getOrder().readQuantity());
        assertEquals(500L, entry.getStatus().readAcceptedTimestamp());
        assertEquals(800L, entry.getStatus().readFilledTimestamp());
    }

    @Test
    void canWriteRepeatedRecordsSameObject()
    {
        OrderBook entry = new OrderBook();
        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(entry.precomputeBufferLength(10));
        entry.setUnderlyingBuffer(buffer, 0);
        entry.resetOrderBookItemSize(10);
        entry.writePair((short)1);
        entry.writeVenue((short)2);

        for (int i = 0; i < 10; i++)
        {
            entry.getOrderBookItem(i).writePrice(i * 100);
            entry.getOrderBookItem(i).writeSize(i * 1000);
        }

        for (int i = 0; i < 10; i++)
        {
            assertEquals(i * 100, entry.getOrderBookItem(i).readPrice());
            assertEquals(i * 1000, entry.getOrderBookItem(i).readSize());
        }
    }

    @Test
    void canWriteRepeatedRecordsNewObject()
    {
        OrderBook entry = new OrderBook();
        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(entry.precomputeBufferLength(10));
        entry.setUnderlyingBuffer(buffer, 0);
        entry.resetOrderBookItemSize(10);
        entry.writePair((short)1);
        entry.writeVenue((short)2);

        for (int i = 0; i < 10; i++)
        {
            entry.getOrderBookItem(i).writePrice(i * 100);
            entry.getOrderBookItem(i).writeSize(i * 1000);
        }

        OrderBook reader = new OrderBook();
        reader.setUnderlyingBuffer(buffer, 0);
        reader.readOrderBookItemSize();
        assertEquals((short)1, reader.readPair());
        assertEquals((short)2, reader.readVenue());

        for (int i = 0; i < 10; i++)
        {
            assertEquals(i * 100, reader.getOrderBookItem(i).readPrice());
            assertEquals(i * 1000, reader.getOrderBookItem(i).readSize());
        }
    }

    @Test
    void canLockTheKey()
    {
        OrderBookEntry entry = new OrderBookEntry();
        entry.writeId(45);
        entry.lockKeyId();
        boolean written = entry.writeId(47);
        assertFalse(written);
        assertEquals(45, entry.readId());
    }

    @Test
    void canReadWriteCompositeExternalBuffer()
    {
        ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(OrderBookEntry.BUFFER_LENGTH);

        OrderBookEntry entry = new OrderBookEntry(buffer, 0);
        entry.getOrder().writeClOrdId(ORDER_123);
        entry.getOrder().writePrice(12300L);
        entry.getOrder().writeQuantity(1_000_000L);
        entry.getStatus().writeAcceptedTimestamp(500L);
        entry.getStatus().writeFilledTimestamp(800L);

        assertEquals(ORDER_123, entry.getOrder().readClOrdId());
        assertEquals(12300L, entry.getOrder().readPrice());
        assertEquals(1_000_000L, entry.getOrder().readQuantity());
        assertEquals(500L, entry.getStatus().readAcceptedTimestamp());
        assertEquals(800L, entry.getStatus().readFilledTimestamp());

        int eiderSpecId = EiderHelper.getEiderId(buffer, 0);
        assertEquals(688, eiderSpecId);
    }

    @Test
    void canCopyFromBuffer()
    {
        ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(Order.BUFFER_LENGTH);
        Order fromNetwork = new Order();
        fromNetwork.setUnderlyingBuffer(buffer, 0);
        fromNetwork.writeQuantity(1_000_000L);
        fromNetwork.writePrice(12300L);
        fromNetwork.writeClOrdId(ORDER_123);
        fromNetwork.writeInstrument(INSTRUMENT);

        OrderBookEntry entry = new OrderBookEntry();
        entry.copyOrderFromBuffer(buffer, 0);
        entry.getStatus().writeAcceptedTimestamp(500L);
        entry.getStatus().writeFilledTimestamp(800L);

        assertEquals(ORDER_123, entry.getOrder().readClOrdId());
        assertEquals(INSTRUMENT, entry.getOrder().readInstrument());
        assertEquals(12300L, entry.getOrder().readPrice());
        assertEquals(1_000_000L, entry.getOrder().readQuantity());
        assertEquals(500L, entry.getStatus().readAcceptedTimestamp());
        assertEquals(800L, entry.getStatus().readFilledTimestamp());
    }

    @Test
    void canPut()
    {
        ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(Order.BUFFER_LENGTH);
        Order fromNetwork = new Order();
        fromNetwork.setUnderlyingBuffer(buffer, 0);
        fromNetwork.writeQuantity(1_000_000L);
        fromNetwork.writePrice(12300L);
        fromNetwork.writeClOrdId(ORDER_123);
        fromNetwork.writeInstrument(INSTRUMENT);

        OrderBookEntry entry = new OrderBookEntry();
        entry.putOrder(fromNetwork);
        entry.getStatus().writeAcceptedTimestamp(500L);
        entry.getStatus().writeFilledTimestamp(800L);

        assertEquals(ORDER_123, entry.getOrder().readClOrdId());
        assertEquals(INSTRUMENT, entry.getOrder().readInstrument());
        assertEquals(12300L, entry.getOrder().readPrice());
        assertEquals(1_000_000L, entry.getOrder().readQuantity());
        assertEquals(500L, entry.getStatus().readAcceptedTimestamp());
        assertEquals(800L, entry.getStatus().readFilledTimestamp());
    }

    @Test
    void repositoryWorks()
    {
        OrderBookEntryRepository repository = OrderBookEntryRepository.createWithCapacity(10);
        OrderBookEntry entry = repository.appendWithKey(1);
        assertNotNull(entry);
        entry.getStatus().writeFilledTimestamp(100L);
        entry.getStatus().writeAcceptedTimestamp(200L);
        entry.getStatus().writeFilled(true);
        entry.getOrder().writeInstrument(INSTRUMENT);
        entry.getOrder().writeClOrdId(ORDER_123);

        boolean containsIt = repository.containsKey(1);
        assertTrue(containsIt);

        OrderBookEntry read = repository.getByKey(1);
        assertNotNull(read);
        assertEquals(100L, read.getStatus().readFilledTimestamp());
        assertEquals(200L, read.getStatus().readAcceptedTimestamp());
        assertTrue(read.getStatus().readFilled());
        assertEquals(INSTRUMENT, read.getOrder().readInstrument());
        assertEquals(ORDER_123, read.getOrder().readClOrdId());
    }

    @Test
    void repositoryIterationWorks()
    {
        OrderBookEntryRepository repository = OrderBookEntryRepository.createWithCapacity(10);
        repository.appendWithKey(0);
        repository.appendWithKey(1);
        repository.appendWithKey(2);
        repository.appendWithKey(3);
        repository.appendWithKey(4);
        repository.appendWithKey(5);
        repository.appendWithKey(6);
        repository.appendWithKey(7);
        repository.appendWithKey(8);
        repository.appendWithKey(9);

        int key = 0;
        while (repository.allItems().hasNext())
        {
            OrderBookEntry item = repository.allItems().next();
            Assertions.assertEquals(key, item.readId());
            key++;
        }
        Assertions.assertEquals(10, key);
    }
}

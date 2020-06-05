package com.foo.sample;

import com.foo.sample.gen.Order;
import com.foo.sample.gen.OrderBookEntry;

import com.foo.sample.gen.OrderBookEntryRepository;

import io.eider.Helper.EiderHelper;

import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompositeTests
{
    public static final String INSTRUMENT = "012345678";
    public static final String ORDER_123 = "ORDER123";

    @Test
    public void canReadWriteCompositeInternalBuffer()
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
    public void canLockTheKey()
    {
        OrderBookEntry entry = new OrderBookEntry();
        entry.writeId(45);
        entry.lockKeyId();
        boolean written = entry.writeId(47);
        assertEquals(false, written);
        assertEquals(45, entry.readId());
    }

    @Test
    public void canReadWriteCompositeExternalBuffer()
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
    public void canCopyFromBuffer()
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
    public void canPut()
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
    public void repositoryWorks()
    {
        OrderBookEntryRepository repository = OrderBookEntryRepository.createWithCapacity(10);
        OrderBookEntry entry = repository.appendWithKey(1);
        entry.getStatus().writeFilledTimestamp(100L);
        entry.getStatus().writeAcceptedTimestamp(200L);
        entry.getStatus().writeFilled(true);
        entry.getOrder().writeInstrument(INSTRUMENT);
        entry.getOrder().writeClOrdId(ORDER_123);

        boolean containsIt = repository.containsKey(1);
        assertTrue(containsIt);

        OrderBookEntry read = repository.getByKey(1);
        assertEquals(100L, read.getStatus().readFilledTimestamp());
        assertEquals(200L, read.getStatus().readAcceptedTimestamp());
        assertTrue(read.getStatus().readFilled());
        assertEquals(INSTRUMENT, read.getOrder().readInstrument());
        assertEquals(ORDER_123, read.getOrder().readClOrdId());
    }

    @Test
    public void repositoryIterationWorks()
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

package com.foo.sample;

import com.foo.sample.gen.EiderHelper;
import com.foo.sample.gen.Order;
import com.foo.sample.gen.OrderBookEntry;

import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        int eiderSpecId = EiderHelper.getEiderSpecId(0, buffer);
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
}

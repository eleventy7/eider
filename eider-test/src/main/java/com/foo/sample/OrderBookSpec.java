package com.foo.sample;

import io.eider.annotation.EiderAttribute;
import io.eider.annotation.EiderSpec;

@EiderSpec(eiderId = 111, name = "OrderBook", header = false)
public class OrderBookSpec
{
    private short pair;
    private short venue;
    @EiderAttribute(repeatedRecord = true)
    private OrderBookItem items;
}

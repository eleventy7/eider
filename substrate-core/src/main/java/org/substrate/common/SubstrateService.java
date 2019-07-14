package org.substrate.common;

public interface SubstrateService
{
    void closing();

    int dutyCycle();

    void onMessage(Object message, String reference);
}

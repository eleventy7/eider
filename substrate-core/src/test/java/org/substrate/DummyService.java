package org.substrate;

import org.substrate.common.SubstrateService;

public class DummyService implements SubstrateService {
    @Override
    public int dutyCycle() {
        return 0;
    }
}

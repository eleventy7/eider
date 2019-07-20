/*
 * Copyright 2019 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.substrate.serialization;

import org.agrona.DirectBuffer;
import org.substrate.common.Substrate;
import org.substrate.common.SubstrateService;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;

public class SubstrateFragmentHandler implements FragmentHandler
{
    private final SubstrateService service;
    private final Substrate substrate;

    public SubstrateFragmentHandler(final SubstrateService service, final Substrate substrate)
    {
        this.service = service;
        this.substrate = substrate;
    }

    @Override
    public void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        //get header
        //deserialize
        //call service.onMessage
    }
}

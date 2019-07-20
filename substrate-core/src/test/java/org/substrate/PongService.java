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

package org.substrate;

import org.substrate.common.SubstrateService;
import org.substrate.serialization.SubstrateMessage;

public class PongService implements SubstrateService
{
    @Override
    public void onStart()
    {

    }

    @Override
    public void closing()
    {

    }

    @Override
    public int dutyCycle()
    {
        return 0;
    }

    @Override
    public void onMessage(final SubstrateMessage message, final String reference)
    {

    }


}

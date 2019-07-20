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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.substrate.common.SendStatus;
import org.substrate.worker.SubstrateService;
import org.substrate.serialization.SubstrateMessage;

public class PongService extends SubstrateService
{
    private static final Logger log = LoggerFactory.getLogger(PongService.class);

    @Override
    public void onStart()
    {
        SendStatus status = send("ping-pong", "pong", new PingMessage());
        log.info("send status for start = {}", status);
    }

    @Override
    public void closing()
    {
        log.info("shutting down");
    }

    @Override
    public void onMessage(final SubstrateMessage message, final String source)
    {
        log.info("got a message!");

    }
}

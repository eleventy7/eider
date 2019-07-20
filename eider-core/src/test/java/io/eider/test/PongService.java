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

package io.eider.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.eider.serialization.EiderMessage;
import io.eider.worker.Service;

public class PongService extends Service
{
    private static final Logger log = LoggerFactory.getLogger(PongService.class);
    private int count;

    @Override
    public void onStart()
    {
        log.info("Starting");
    }

    @Override
    public void closing()
    {
        log.info("Closing");
    }

    @Override
    public void onMessage(final EiderMessage message, int messageType, final String source)
    {
        log.info("pong");
        count++;
        send("ping-pong", "ping", new PingMessage());
    }

    public int getCount()
    {
        return count;
    }
}

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

package io.eider.worker;

import io.eider.common.SendStatus;
import io.eider.serialization.EiderMessage;

public abstract class Service
{
    private static final int NOTHING_TO_DO = 0;
    private Worker worker;

    public abstract void onStart();

    public abstract void onClose();

    int dutyCycle()
    {
        return NOTHING_TO_DO;
    }

    public abstract void onMessage(EiderMessage message, int messageType, String conduit, String from);

    void setWorker(final Worker worker)
    {
        this.worker = worker;
    }

    protected SendStatus send(String conduit, String destination, int messageType, EiderMessage message)
    {
        return worker.send(conduit, destination, messageType, message);
    }

    protected SendStatus broadcast(int messageType, EiderMessage message)
    {
        return worker.broadcast(messageType, message);
    }
}

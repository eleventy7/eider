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

package io.eider.common;

public enum SendStatus
{
    OK,
    BACK_PRESSURE,
    NOT_CONNECTED,
    UNKNOWN;

    public static SendStatus fromOffer(final long offer)
    {
        if (offer > 0L)
        {
            return OK;
        }
        else if (offer == -2 || offer == -3)
        {
            return BACK_PRESSURE;
        }
        else if (offer == -1)
        {
            return NOT_CONNECTED;
        }
        else
        {
            return UNKNOWN;
        }
    }
}

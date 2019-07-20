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

package io.eider.serialization;

public class SerializationResponse
{
    private int type;
    private byte[] data;

    public SerializationResponse(final int type, final byte[] data)
    {
        this.type = type;
        this.data = data;
    }

    public int getType()
    {
        return type;
    }

    public byte[] getData()
    {
        return data;
    }
}

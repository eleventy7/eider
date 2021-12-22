/*
 * Copyright ©2019-2022 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.foo.sample;

import com.foo.sample.gen.EiderObject;
import com.foo.sample.gen.EiderObjectRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IteratorTests
{
    @Test
    void canIterate()
    {
        EiderObjectRepository underTest = EiderObjectRepository.createWithCapacity(10);

        underTest.appendWithKey(0);
        underTest.appendWithKey(1);
        underTest.appendWithKey(2);
        underTest.appendWithKey(3);
        underTest.appendWithKey(4);
        underTest.appendWithKey(5);
        underTest.appendWithKey(6);
        underTest.appendWithKey(7);
        underTest.appendWithKey(8);
        underTest.appendWithKey(9);

        int key = 0;
        while (underTest.allItems().hasNext())
        {
            EiderObject next = underTest.allItems().next();
            Assertions.assertEquals(key, next.readId());
            key++;
        }
        Assertions.assertEquals(10, key);

    }
}

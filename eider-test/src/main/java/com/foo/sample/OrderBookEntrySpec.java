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

import io.eider.annotation.EiderAttribute;
import io.eider.annotation.EiderRepository;
import io.eider.annotation.EiderComposite;

@EiderComposite(name = "OrderBookEntry", eiderId = 688)
@EiderRepository
public class OrderBookEntrySpec
{
    @EiderAttribute(key = true)
    long id;
    OrderSpec order;
    OrderBookStatusSpec status;
}

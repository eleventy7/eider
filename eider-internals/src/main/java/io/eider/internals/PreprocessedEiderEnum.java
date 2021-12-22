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

package io.eider.internals;

import java.util.ArrayList;
import java.util.List;

public class PreprocessedEiderEnum
{
    private final String name;
    private final RepresentationType representationType;
    private final List<PreprocessedEiderEnumItem> items;

    public PreprocessedEiderEnum(String name, RepresentationType representationType)
    {
        this.name = name;
        this.representationType = representationType;
        this.items = new ArrayList<PreprocessedEiderEnumItem>();
    }

    public List<PreprocessedEiderEnumItem> getItems()
    {
        return this.items;
    }

    public String getName()
    {
        return this.name;
    }

    public RepresentationType getRepresentationType()
    {
        return representationType;
    }
}

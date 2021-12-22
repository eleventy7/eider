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

package io.eider.parser.internals;

import java.util.List;

public class ParsedField
{
    private final String name;
    private final String typeString;
    private final ParsedDataType typeEnum;
    private final int order;
    private final List<ParsedAttribute> attributes;

    public ParsedField(final String name,
                       final String typeString,
                       final ParsedDataType typeEnum,
                       final int order,
                       final List<ParsedAttribute> attributes)
    {
        this.name = name;
        this.typeEnum = typeEnum;
        this.typeString = typeString;
        this.order = order;
        this.attributes = attributes;
    }

    public String getName()
    {
        return name;
    }

    public String getTypeString()
    {
        return typeString;
    }

    public ParsedDataType getTypeEnum()
    {
        return typeEnum;
    }

    public int getOrder()
    {
        return order;
    }

    public List<ParsedAttribute> getAttributes()
    {
        return attributes;
    }
}

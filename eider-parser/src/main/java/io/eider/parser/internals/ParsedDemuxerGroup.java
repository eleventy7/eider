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

public class ParsedDemuxerGroup
{

    private final String name;
    private final int groupId;
    private final List<ParsedField> fields;

    public ParsedDemuxerGroup(final String name,
                              final int groupId,
                              final List<ParsedField> fields)
    {
        this.name = name;
        this.groupId = groupId;
        this.fields = fields;
    }

    public String getName()
    {
        return name;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public List<ParsedField> getFields()
    {
        return fields;
    }
}

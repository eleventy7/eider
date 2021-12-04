package io.eider.internals;

/*
 * Copyright 2019-2021 Shaun Laurens.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;

public class PreprocessedEiderProperty
{
    private final String name;
    private final EiderPropertyType type;
    private final String recordType;
    private final Map<String, String> annotations;

    public PreprocessedEiderProperty(final String name,
                                     final EiderPropertyType type,
                                     final String recordType,
                                     final Map<String, String> annotations)
    {
        this.name = name;
        this.type = type;
        this.recordType = recordType;
        this.annotations = annotations;
    }

    public String getName()
    {
        return name;
    }

    public EiderPropertyType getType()
    {
        return type;
    }

    public String getRecordType()
    {
        return recordType;
    }

    public Map<String, String> getAnnotations()
    {
        return annotations;
    }
}

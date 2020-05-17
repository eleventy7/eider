/*
 * Copyright 2019-2020 Shaun Laurens.
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

package io.eider.processor;

import java.util.List;

public class PreprosssedEiderObject
{
    private final String name;
    private final String classNameInput;
    private final int sequence;
    private final String packageNameGen;
    private final List<PreprocessedEiderProperty> propertyList;

    public PreprosssedEiderObject(final String name, final String classNameInput,
                                  final int sequence, final String packageNameGen,
                                  final boolean fixedLength,
                                  final List<PreprocessedEiderProperty> propertyList)
    {
        this.name = name;
        this.classNameInput = classNameInput;
        this.sequence = sequence;
        this.packageNameGen = packageNameGen;
        this.propertyList = propertyList;
    }

    public String getName()
    {
        return name;
    }

    public String getClassNameInput()
    {
        return classNameInput;
    }

    public List<PreprocessedEiderProperty> getPropertyList()
    {
        return propertyList;
    }

    public int getSequence()
    {
        return sequence;
    }

    public String getPackageNameGen()
    {
        return packageNameGen;
    }
}

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

import java.util.List;

public class PreprocessedEiderComposite
{
    private final String name;
    private final String classNameInput;
    private final short eiderId;
    private final String packageNameGen;
    private final boolean buildRepository;
    private final String repositoryName;
    private final String keyName;
    private final EiderPropertyType keyType;
    private final List<PreprocessedNamedEiderObject> objectList;

    @SuppressWarnings("all")
    public PreprocessedEiderComposite(final String name, final String classNameInput,
                                      final short eiderId, final String packageNameGen,
                                      final boolean buildRepository, final String repositoryName,
                                      final String keyName, final EiderPropertyType keyType,
                                      final List<PreprocessedNamedEiderObject> objectList)
    {
        this.name = name;
        this.classNameInput = classNameInput;
        this.eiderId = eiderId;
        this.packageNameGen = packageNameGen;
        this.buildRepository = buildRepository;
        this.repositoryName = repositoryName;
        this.keyName = keyName;
        this.keyType = keyType;
        this.objectList = objectList;
    }

    public String getName()
    {
        return name;
    }

    public String getClassNameInput()
    {
        return classNameInput;
    }

    public List<PreprocessedNamedEiderObject> getObjectList()
    {
        return objectList;
    }

    public short getEiderId()
    {
        return eiderId;
    }

    public String getPackageNameGen()
    {
        return packageNameGen;
    }

    public boolean buildRepository()
    {
        return buildRepository;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public EiderPropertyType getKeyType()
    {
        return keyType;
    }

    public String getKeyName()
    {
        return keyName;
    }
}

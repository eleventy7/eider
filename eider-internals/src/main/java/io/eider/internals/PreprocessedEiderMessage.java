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

public class PreprocessedEiderMessage
{
    private final String name;
    private final String classNameInput;
    private final short eiderId;
    private final short eiderGroupId;
    private final String packageNameGen;
    private final boolean fixedLength;
    private final boolean buildRepository;
    private final boolean buildHeader;
    private final String repositoryName;
    private final boolean transactional;
    private final boolean transactionalRepository;
    private final List<PreprocessedEiderProperty> propertyList;

    public PreprocessedEiderMessage(final String name, final String classNameInput,
                                    final short eiderId, final short eiderGroupId,
                                    final String packageNameGen,
                                    final boolean fixedLength,
                                    final boolean buildRepository, final String repositoryName,
                                    final boolean transactional, final boolean transactionalRepository,
                                    final boolean buildHeader, final List<PreprocessedEiderProperty> propertyList)
    {
        this.name = name;
        this.classNameInput = classNameInput;
        this.eiderId = eiderId;
        this.eiderGroupId = eiderGroupId;
        this.packageNameGen = packageNameGen;
        this.fixedLength = fixedLength;
        this.buildRepository = buildRepository;
        this.repositoryName = repositoryName;
        this.transactional = transactional;
        this.transactionalRepository = transactionalRepository;
        this.propertyList = propertyList;
        this.buildHeader = buildHeader;
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

    public boolean isTransactional()
    {
        return transactional;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public boolean isFixedLength()
    {
        return fixedLength;
    }

    public short getEiderGroupId()
    {
        return eiderGroupId;
    }

    public boolean isTransactionalRepository()
    {
        return transactionalRepository;
    }

    public boolean mustBuildHeader()
    {
        return buildHeader;
    }
}

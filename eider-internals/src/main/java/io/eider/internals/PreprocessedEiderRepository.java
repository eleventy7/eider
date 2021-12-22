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

import java.util.List;

public class PreprocessedEiderRepository
{
    private final String repositoryName;
    private final boolean transactional;
    private final boolean diffTracking;
    private final boolean snapshotable;
    private final List<PreprocessedEiderProperty> propertyList;

    public PreprocessedEiderRepository(final String repositoryName,
                                       final boolean transactional,
                                       final boolean diffTracking,
                                       final boolean snapshotable,
                                       final List<PreprocessedEiderProperty> propertyList)
    {
        this.repositoryName = repositoryName;
        this.transactional = transactional;
        this.diffTracking = diffTracking;
        this.snapshotable = snapshotable;
        this.propertyList = propertyList;
    }

    public List<PreprocessedEiderProperty> getPropertyList()
    {
        return propertyList;
    }

    public boolean isTransactional()
    {
        return transactional;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public boolean isDiffTracking()
    {
        return diffTracking;
    }

    public boolean isSnapshotable()
    {
        return snapshotable;
    }
}

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

package io.eider.javawriter;

import io.eider.internals.PreprocessedEiderComposite;
import io.eider.internals.PreprocessedEiderMessage;
import io.eider.internals.PreprocessedEiderRepeatableRecord;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.List;

public interface EiderCodeWriter
{
    void generate(ProcessingEnvironment pe,
                  List<PreprocessedEiderRepeatableRecord> records,
                  List<PreprocessedEiderMessage> objects,
                  List<PreprocessedEiderComposite> composites);
}

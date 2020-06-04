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

public enum EiderPropertyType
{
    BOOLEAN,
    CHAR8,
    SHORT,
    INT,
    LONG,
    FIXED_STRING,
    VAR_STRING,
    INVALID;

    public static EiderPropertyType from(String toString)
    {
        switch (toString)
        {
            case "int":
                return INT;
            case "short":
                return SHORT;
            case "char":
                return CHAR8;
            case "long":
                return LONG;
            case "boolean":
                return BOOLEAN;
            default:
                return INVALID;
        }
    }

}

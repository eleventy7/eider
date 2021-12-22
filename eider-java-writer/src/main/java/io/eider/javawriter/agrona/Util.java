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

package io.eider.javawriter.agrona;

import java.util.Map;

import io.eider.javawriter.agrona.AttributeConstants;
import io.eider.internals.EiderPropertyType;

public final class Util
{
    private Util()
    {
        //not used
    }

    public static String upperFirst(String input)
    {
        if (input == null)
        {
            throw new AgronaWriterException("Illegal input for upperFirst");
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    @SuppressWarnings("all")
    public static Class fromType(EiderPropertyType type)
    {
        switch (type)
        {
            case SHORT:
                return short.class;
            case LONG:
                return long.class;
            case BOOLEAN:
                return boolean.class;
            case FIXED_STRING:
                return String.class;
            case DOUBLE:
                return Double.class;
            default:
                return int.class;
        }
    }

    @SuppressWarnings("all")
    public static Class getBoxedType(EiderPropertyType type)
    {
        switch (type)
        {
            case SHORT:
                return Short.class;
            case LONG:
                return Long.class;
            case BOOLEAN:
                return Boolean.class;
            case FIXED_STRING:
                return String.class;
            case DOUBLE:
                return Double.class;
            default:
                return Integer.class;
        }
    }


    public static String getComparator(EiderPropertyType type, String sourceValue)
    {
        switch (type)
        {
            case BOOLEAN:
                return "booleanValue() == " + sourceValue;
            case FIXED_STRING:
                return "equalsIgnoreCase(value)";
            default:
                return "equals(value)";
        }
    }


    public static int byteLength(EiderPropertyType type, Map<String, String> annotations)
    {
        switch (type)
        {
            case LONG:
                return Long.BYTES;
            case BOOLEAN:
                return 1;
            case SHORT:
                return Short.BYTES;
            case DOUBLE:
                return Double.BYTES;
            case FIXED_STRING:
                return Integer.parseInt(annotations.get(AttributeConstants.MAXLENGTH));
            default:
                return Integer.BYTES;
        }
    }

    public static String fromTypeToStr(EiderPropertyType type)
    {
        switch (type)
        {
            case INT:
                return "int";
            case LONG:
                return "long";
            case SHORT:
                return "short";
            case BOOLEAN:
                return "boolean";
            case FIXED_STRING:
                return "String";
            case DOUBLE:
                return "double";
            default:
                return "invalid";
        }
    }
}

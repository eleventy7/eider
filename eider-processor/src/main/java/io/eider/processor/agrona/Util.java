package io.eider.processor.agrona;

import java.util.Map;

import io.eider.processor.AttributeConstants;
import io.eider.processor.EiderPropertyType;

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
            default:
                return int.class;
        }
    }

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
            default:
                return Integer.class;
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
            default:
                return "invalid";
        }
    }
}

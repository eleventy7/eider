package io.eider.parser.internals;

public enum ParsedDataType
{
    RECORD,
    ENUM,
    INT16,
    INT32,
    INT64,
    DOUBLE,
    BOOLEAN,
    STRING,
    UNKNOWN;

    public static ParsedDataType fromString(final String parsedString)
    {
        return switch (parsedString.toLowerCase())
        {
            case "int16" -> INT16;
            case "int32" -> INT32;
            case "int64" -> INT64;
            case "double" -> DOUBLE;
            case "boolean" -> BOOLEAN;
            case "string" -> STRING;
            default -> UNKNOWN;
        };
    }
}

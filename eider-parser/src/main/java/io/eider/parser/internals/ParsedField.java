package io.eider.parser.internals;

import java.util.List;

public class ParsedField
{
    private final String name;
    private final String typeString;
    private final ParsedDataType typeEnum;
    private final List<ParsedAttribute> attributes;

    public ParsedField(final String name,
                       final String typeString,
                       final ParsedDataType typeEnum,
                       final int order,
                       final List<ParsedAttribute> attributes)
    {
        this.name = name;
        this.typeEnum = typeEnum;
        this.typeString = typeString;
        this.attributes = attributes;
    }

    public String getName()
    {
        return name;
    }

    public String getTypeString()
    {
        return typeString;
    }

    public ParsedDataType getTypeEnum()
    {
        return typeEnum;
    }

    public List<ParsedAttribute> getAttributes()
    {
        return attributes;
    }
}

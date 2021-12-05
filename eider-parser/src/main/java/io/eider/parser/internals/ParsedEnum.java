package io.eider.parser.internals;

import java.util.List;

public class ParsedEnum
{
    public final String name;
    public final List<ParsedEnumItem> enumItemList;
    public final List<ParsedAttribute> attributes;

    public ParsedEnum(final String name,
                      final List<ParsedEnumItem> enumItemList,
                      final List<ParsedAttribute> attributes)
    {
        this.name = name;
        this.enumItemList = enumItemList;
        this.attributes = attributes;
    }

    public String getName()
    {
        return name;
    }

    public List<ParsedEnumItem> getEnumItemList()
    {
        return enumItemList;
    }

    public List<ParsedAttribute> getAttributes()
    {
        return attributes;
    }
}

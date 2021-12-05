package io.eider.parser.internals;

import java.util.List;

public class ParsedAttribute
{
    final String name;
    final boolean valid;
    final List<ParsedAttributeItem> attributeItems;

    public ParsedAttribute(String name, final boolean valid, List<ParsedAttributeItem> attributeItems)
    {
        this.name = name;
        this.valid = valid;
        this.attributeItems = attributeItems;
    }

    public boolean isValid()
    {
        return valid;
    }

    public String getName()
    {
        return name;
    }

    public List<ParsedAttributeItem> getAttributeItems()
    {
        return attributeItems;
    }
}

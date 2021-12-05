package io.eider.parser.internals;

public class ParsedAttributeItem
{
    private final String name;
    private final String value;

    public ParsedAttributeItem(final String name, final String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return this.name;
    }

    public String getValue()
    {
        return this.value;
    }
}

package io.eider.parser.internals;

public class ParsedEnumItem
{
    private final String name;
    private final String representation;

    public ParsedEnumItem(final String name, final String representation)
    {
        this.name = name;
        this.representation = representation;
    }

    public String getName()
    {
        return name;
    }

    public String getRepresentation()
    {
        return representation;
    }
}

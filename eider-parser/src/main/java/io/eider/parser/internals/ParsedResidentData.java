package io.eider.parser.internals;

import java.util.List;

public class ParsedResidentData
{
    public final String name;
    public final List<ParsedAttribute> attributes;
    private final List<ParsedField> fields;

    public ParsedResidentData(final String name,
                              final List<ParsedAttribute> attributes,
                              final List<ParsedField> fields)
    {
        this.name = name;
        this.attributes = attributes;
        this.fields = fields;
    }

    public String getName()
    {
        return name;
    }

    public List<ParsedAttribute> getAttributes()
    {
        return attributes;
    }

    public List<ParsedField> getFields()
    {
        return fields;
    }
}

package io.eider.parser.internals;

import java.util.List;

public class ParsedDemuxerGroup
{

    private final String name;
    private final int groupId;
    private final List<ParsedField> fields;

    public ParsedDemuxerGroup(final String name,
                              final int groupId,
                              final List<ParsedField> fields)
    {
        this.name = name;
        this.groupId = groupId;
        this.fields = fields;
    }

    public String getName()
    {
        return name;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public List<ParsedField> getFields()
    {
        return fields;
    }
}

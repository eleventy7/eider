package io.eider.parser.internals;

import java.util.List;

public class ParsedDemuxer
{
    private final String name;
    private final List<ParsedAttribute> attributes;
    private final List<ParsedDemuxer> demuxers;

    public ParsedDemuxer(final String name,
                         final List<ParsedAttribute> attributes,
                         final List<ParsedDemuxer> demuxers)
    {
        this.name = name;
        this.attributes = attributes;
        this.demuxers = demuxers;
    }

    public String getName()
    {
        return name;
    }

    public List<ParsedAttribute> getAttributes()
    {
        return attributes;
    }

    public List<ParsedDemuxer> getDemuxers()
    {
        return demuxers;
    }
}

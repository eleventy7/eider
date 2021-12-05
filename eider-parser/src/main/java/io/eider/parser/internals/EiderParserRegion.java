package io.eider.parser.internals;

public class EiderParserRegion
{
    private final int start;
    private final int end;
    private final EiderParserRegionType type;

    public EiderParserRegion(final int start, final int end, final EiderParserRegionType type)
    {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public int getStart()
    {
        return this.start;
    }

    public int getEnd()
    {
        return end;
    }

    public EiderParserRegionType getType()
    {
        return type;
    }
}

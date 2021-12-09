package io.eider.parser.internals;

public class ParsedFieldRegion
{
    private final int start;
    private final int end;

    public ParsedFieldRegion(final int start, final int end)
    {
        this.start = start;
        this.end = end;
    }

    public int getStart()
    {
        return start;
    }

    public int getEnd()
    {
        return end;
    }
}

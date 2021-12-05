package io.eider.parser.internals;

public class InputLine
{
    private final int lineNumber;
    private final String contents;

    public InputLine(final int lineNumber, final String contents)
    {
        this.lineNumber = lineNumber;
        this.contents = contents;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public String getContents()
    {
        return contents;
    }
}

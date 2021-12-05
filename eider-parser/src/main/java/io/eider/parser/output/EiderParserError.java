package io.eider.parser.output;

public class EiderParserError
{
    private final int line;
    private final int column;
    private final String message;
    private ParserIssueType type;

    public EiderParserError(final int line, final int column, final String message, final ParserIssueType type)
    {
        this.line = line;
        this.column = column;
        this.message = message;
        this.type = type;
    }

    public int getLine()
    {
        return this.line;
    }

    public int getColumn()
    {
        return column;
    }

    public String getMessage()
    {
        return message;
    }

    public ParserIssueType getType()
    {
        return type;
    }
}

package io.eider.parser.output;

public class EiderOutputFile
{
    private final String filename;
    private final String contents;

    public EiderOutputFile(final String filename, final String contents)
    {
        this.filename = filename;
        this.contents = contents;
    }

    public String getContents()
    {
        return contents;
    }

    public String getFilename()
    {
        return this.filename;
    }
}

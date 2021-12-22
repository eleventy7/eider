package io.eider.parser.internals;

public class ParsedEiderBlock
{
    public final String packageName;
    public final String version;

    public ParsedEiderBlock(final String packageName,
                            final String version)
    {
        this.packageName = packageName;
        this.version = version;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public String getVersion()
    {
        return version;
    }
}

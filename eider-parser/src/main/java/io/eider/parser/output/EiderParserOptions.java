package io.eider.parser.output;

public class EiderParserOptions
{
    private final String packageName;
    private final String eiderVersion;

    public EiderParserOptions(final String packageName, final String eiderVersion)
    {
        this.packageName = packageName;
        this.eiderVersion = eiderVersion;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public String getEiderVersion()
    {
        return eiderVersion;
    }
}

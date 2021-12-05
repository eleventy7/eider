package io.eider.internals;

public class PreprocessedEiderEnumItem
{
    private final String name;
    private final String value;

    public PreprocessedEiderEnumItem(final String name, final String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return this.name;
    }

    public String getValue()
    {
        return value;
    }
}

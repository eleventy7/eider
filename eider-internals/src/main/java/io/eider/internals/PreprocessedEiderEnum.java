package io.eider.internals;

import java.util.ArrayList;
import java.util.List;

public class PreprocessedEiderEnum
{
    private final String name;
    private final RepresentationType representationType;
    private final List<PreprocessedEiderEnumItem> items;

    public PreprocessedEiderEnum(String name, RepresentationType representationType)
    {
        this.name = name;
        this.representationType = representationType;
        this.items = new ArrayList<PreprocessedEiderEnumItem>();
    }

    public List<PreprocessedEiderEnumItem> getItems()
    {
        return this.items;
    }

    public String getName()
    {
        return this.name;
    }

    public RepresentationType getRepresentationType()
    {
        return representationType;
    }
}

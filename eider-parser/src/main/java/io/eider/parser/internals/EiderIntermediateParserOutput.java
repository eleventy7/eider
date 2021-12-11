package io.eider.parser.internals;

import io.eider.parser.output.EiderParserError;
import io.eider.parser.output.EiderParserOptions;

import java.util.ArrayList;
import java.util.List;

public class EiderIntermediateParserOutput
{
    private final EiderParserOptions options;
    private final List<ParsedEnum> enums;
    private final List<ParsedRecord> records;
    private final List<ParsedMessage> messages;
    private final List<ParsedResidentData> residentDataList;
    private final List<EiderParserError> errors;
    private boolean success;

    public EiderIntermediateParserOutput(EiderParserOptions options)
    {
        this.success = false;
        this.options = options;
        this.enums = new ArrayList<>();
        this.records = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.residentDataList = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(final boolean success)
    {
        this.success = success;
    }

    public EiderParserOptions getOptions()
    {
        return options;
    }

    public List<ParsedEnum> getEnums()
    {
        return enums;
    }

    public List<ParsedRecord> getRecords()
    {
        return records;
    }

    public List<ParsedMessage> getMessages()
    {
        return messages;
    }

    public List<ParsedResidentData> getResidentDataList()
    {
        return residentDataList;
    }

    public List<EiderParserError> getErrors()
    {
        return errors;
    }

}

package io.eider.parser.output;

import java.util.ArrayList;
import java.util.List;

import io.eider.internals.PreprocessedEiderEnum;
import io.eider.internals.PreprocessedEiderMessage;
import io.eider.internals.PreprocessedEiderRepeatableRecord;
import io.eider.internals.PreprocessedEiderRepository;

public class EiderParserOutput
{
    private final boolean success;
    private final EiderParserOptions options;
    private final List<PreprocessedEiderEnum> enums;
    private final List<PreprocessedEiderRepeatableRecord> records;
    private final List<PreprocessedEiderMessage> messages;
    private final List<PreprocessedEiderRepository> repositories;
    private final List<EiderParserError> errors;

    private List<EiderParserOutput> outputFiles;

    public EiderParserOutput(boolean success,
                             EiderParserOptions options,
                             List<PreprocessedEiderEnum> enums,
                             List<PreprocessedEiderRepeatableRecord> records,
                             List<PreprocessedEiderMessage> messages,
                             List<PreprocessedEiderRepository> repositories,
                             List<EiderParserError> errors)
    {
        this.success = success;
        this.options = options;
        this.enums = enums;
        this.records = records;
        this.messages = messages;
        this.repositories = repositories;
        this.errors = errors;
        this.outputFiles = new ArrayList<>();
    }

    public void appendOutputFile(EiderParserOutput outputFile)
    {
        this.outputFiles.add(outputFile);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public EiderParserOptions getOptions()
    {
        return options;
    }

    public List<PreprocessedEiderEnum> getEnums()
    {
        return enums;
    }

    public List<PreprocessedEiderRepeatableRecord> getRecords()
    {
        return records;
    }

    public List<PreprocessedEiderMessage> getMessages()
    {
        return messages;
    }

    public List<PreprocessedEiderRepository> getRepositories()
    {
        return repositories;
    }

    public List<EiderParserError> getErrors()
    {
        return errors;
    }

    public List<EiderParserOutput> getOutputFiles()
    {
        return outputFiles;
    }
}

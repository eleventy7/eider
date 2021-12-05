package io.eider.parser.output;

import java.util.List;

public class EiderParserErrors
{
    private final List<EiderParserError> errors;

    public EiderParserErrors(List<EiderParserError> errors)
    {
        this.errors = errors;
    }

    public List<EiderParserError> getErrors()
    {
        return errors;
    }
}

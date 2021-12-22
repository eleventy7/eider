package io.eider.parser;

import io.eider.parser.internals.EiderIntermediateParserOutput;
import io.eider.parser.internals.EiderParserRegion;
import io.eider.parser.internals.EiderParserRegionType;
import io.eider.parser.internals.EiderParserUtils;
import io.eider.parser.internals.InputLine;
import io.eider.parser.internals.ParsedEnum;
import io.eider.parser.internals.ParsedMessage;
import io.eider.parser.internals.ParsedRecord;
import io.eider.parser.internals.ParsedResidentData;
import io.eider.parser.output.EiderParserError;
import io.eider.parser.output.EiderParserOutput;
import io.eider.parser.output.ParserIssueType;
import io.eider.parser.validator.EiderParserValidator;

import java.util.List;

public class EiderParser
{
    public EiderParserOutput parse(String input)
    {
        final EiderIntermediateParserOutput output = new EiderIntermediateParserOutput(null);
        parseInternal(input, output);
        validateIntermediate(output);
        return convertToOutput(output);
    }

    private EiderParserOutput convertToOutput(EiderIntermediateParserOutput output)
    {
        return null;
    }

    private void validateIntermediate(EiderIntermediateParserOutput output)
    {
        if (hasFatalErrors(output))
        {
            return;
        }
        EiderParserValidator.validate(output);

    }

    /**
     * Forgiving parser which allows enums and records to be in any order.
     * @param input raw body of text
     * @param output intermediate parser output to be filled
     */
    private void parseInternal(String input, EiderIntermediateParserOutput output)
    {
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, output.getErrors());

        //need to first parse all enums
        for (EiderParserRegion eiderParserRegion : eiderParserRegions)
        {
            if (eiderParserRegion.getType() == EiderParserRegionType.ENUM)
            {
                ParsedEnum parsedEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegion.getStart(),
                    eiderParserRegion.getEnd(), output.getErrors());
                if (parsedEnum != null)
                {
                    output.getEnums().add(parsedEnum);
                }
            }
        }

        //then all records
        for (EiderParserRegion eiderParserRegion : eiderParserRegions)
        {
            if (eiderParserRegion.getType() == EiderParserRegionType.RECORD)
            {
                ParsedRecord parsedRecord = EiderParserUtils.parseRecord(inputLines, eiderParserRegion.getStart(),
                    eiderParserRegion.getEnd(), output.getEnums(), output.getErrors());
                if (parsedRecord != null)
                {
                    output.getRecords().add(parsedRecord);
                }
            }
        }

        //then the messages and resident data
        for (EiderParserRegion eiderParserRegion : eiderParserRegions)
        {
            if (eiderParserRegion.getType() == EiderParserRegionType.MESSAGE)
            {
                ParsedMessage parsedMessage = EiderParserUtils.parseMessage(inputLines, eiderParserRegion.getStart(),
                    eiderParserRegion.getEnd(), output.getEnums(), output.getRecords(), output.getErrors());
                if (parsedMessage != null)
                {
                    output.getMessages().add(parsedMessage);
                }
            }

            if (eiderParserRegion.getType() == EiderParserRegionType.RESIDENT_DATA)
            {
                ParsedResidentData parsedResidentData = EiderParserUtils.parseResident(inputLines,
                    eiderParserRegion.getStart(), eiderParserRegion.getEnd(), output.getEnums(),
                    output.getRecords(), output.getErrors());
                if (parsedResidentData != null)
                {
                    output.getResidentDataList().add(parsedResidentData);
                }
            }
        }

        output.setSuccess(hasFatalErrors(output));
    }

    private void validateInput(String input, final EiderIntermediateParserOutput output)
    {

    }

    private boolean hasFatalErrors(final EiderIntermediateParserOutput output)
    {
        for (EiderParserError eiderParserError : output.getErrors())
        {
            if (eiderParserError.getType() == ParserIssueType.FATAL)
            {
                return true;
            }
        }
        return false;
    }
}

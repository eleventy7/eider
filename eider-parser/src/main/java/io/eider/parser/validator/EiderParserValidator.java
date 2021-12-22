package io.eider.parser.validator;

import io.eider.parser.internals.EiderIntermediateParserOutput;

public class EiderParserValidator
{
    public static void validate(EiderIntermediateParserOutput output)
    {
        //check demuxer rules
        validateDemuxerRules(output);
    }

    private static void validateDemuxerRules(EiderIntermediateParserOutput output)
    {
        //

    }

}

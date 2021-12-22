package io.eider.parser.validator;

import io.eider.parser.internals.EiderIntermediateParserOutput;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ValidatorHelpers
{
    private ValidatorHelpers()
    {
        // Private constructor to prevent instantiation
    }

    public static boolean atLeastOneRecordWithAttributeName(EiderIntermediateParserOutput output,
                                                            String attributeName)
    {
        AtomicBoolean result = new AtomicBoolean(false);

        output.getRecords().forEach(parsedRecord ->
        {
            parsedRecord.getAttributes().forEach(attribute ->
            {
                if (attribute.getName().equals(attributeName))
                {
                    result.set(true);
                }
            });
        });

        return result.get();
    }

    public static boolean atLeastOneRecordWithAFieldWithAttributeName(EiderIntermediateParserOutput output,
                                                                      String attributeName)
    {
        AtomicBoolean result = new AtomicBoolean(false);

        output.getRecords().forEach(parsedRecord ->
            parsedRecord.getFields().forEach(field ->
                field.getAttributes().forEach(attribute ->
                {
                    if (attribute.getName().equals(attributeName))
                    {
                        result.set(true);
                    }
                })));

        return result.get();
    }

    public static boolean atLeastOneMessageWithAFieldWithAttributeName(EiderIntermediateParserOutput output,
                                                                       String attributeName)
    {
        AtomicBoolean result = new AtomicBoolean(false);

        output.getMessages().forEach(message ->
            message.getFields().forEach(field ->
                field.getAttributes().forEach(attribute ->
                {
                    if (attribute.getName().equals(attributeName))
                    {
                        result.set(true);
                    }
                })));

        return result.get();
    }

    public static boolean atLeastOneMessageWithAttributeName(EiderIntermediateParserOutput output,
                                                             String attributeName)
    {
        AtomicBoolean result = new AtomicBoolean(false);

        output.getMessages().forEach(message ->
            message.getAttributes().forEach(attribute ->
            {
                if (attribute.getName().equals(attributeName))
                {
                    result.set(true);
                }
            }));

        return result.get();
    }

    public static boolean atLeastOneResidentDataWithAttributeName(EiderIntermediateParserOutput output,
                                                                  String attributeName)
    {
        AtomicBoolean result = new AtomicBoolean(false);

        output.getResidentDataList().forEach(residentData ->
            residentData.getAttributes().forEach(attribute ->
            {
                if (attribute.getName().equals(attributeName))
                {
                    result.set(true);
                }
            }));

        return result.get();
    }
}

package io.eider.parser.internals;

import io.eider.parser.output.EiderParserError;
import io.eider.parser.output.ParserIssueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EiderParserUtils
{

    private static final String RECORD = "record";
    private static final String MESSAGE = "message";
    private static final String RESIDENT_DATA = "resident";
    private static final String EIDER = "eider";
    private static final String ENUM = "enum";
    private static final String DEMUXER = "demuxer";

    private EiderParserUtils()
    {
        // no-op
    }

    /**
     * Takes in string input, and outputs a list of interesting lines including content and original input line number.
     *
     * @param input the raw string input of an eider file
     * @return List of lines that are not empty, and not comments and have leading spaces removed.
     */
    public static List<InputLine> splitInputIntoUsefulLines(String input)
    {
        final List<InputLine> workingLines = new ArrayList<>();
        final String inputWithoutCarriageReturn = removeCarriageReturn(input);
        final String[] linesArray = inputWithoutCarriageReturn.split("\n");

        for (int i = 0; i < linesArray.length; i++)
        {
            final String line = linesArray[i].trim();

            if (!line.isEmpty() && !line.startsWith("//"))
            {
                workingLines.add(new InputLine(i, line));
            }
        }

        return workingLines;
    }

    private static String removeCarriageReturn(String input)
    {
        return input.replace("\r", "");
    }

    private static boolean isAttribute(String input)
    {
        return input.startsWith("@");
    }

    private static boolean isEndOfBlock(String input)
    {
        return input.startsWith("}");
    }

    private static int backtrackToLast(final List<InputLine> inputLines, final int currentIndex)
    {
        int currentLine = currentIndex;
        while (currentLine > 0 && !isEndOfBlock(inputLines.get(currentLine).getContents()))
        {
            currentLine--;
        }

        return currentLine;
    }

    private static int moveForwardToEndOfBlock(final List<InputLine> inputLines, final int currentIndex)
    {
        int currentLine = currentIndex;
        while (currentLine < inputLines.size() && !isEndOfBlock(inputLines.get(currentLine).getContents()))
        {
            currentLine++;
        }

        return currentLine;
    }

    private static boolean lastBlockParsed(final List<InputLine> inputLines, final int currentIndex)
    {
        int currentLine = currentIndex;
        boolean closingCurlyBraceFound = false;
        while (currentLine < inputLines.size())
        {
            if (isEndOfBlock(inputLines.get(currentLine).getContents()))
            {
                closingCurlyBraceFound = true;
                break;
            }
            currentLine++;
        }

        return closingCurlyBraceFound;
    }

    private static void dump(List<InputLine> inputLines)
    {
        for (int i = 0; i < inputLines.size(); i++)
        {
            final InputLine line = inputLines.get(i);
            System.out.println("[" + line.getLineNumber() + "@" + i + "]" + line.getContents());
        }
    }

    public static List<EiderParserRegion> extractRegions(List<InputLine> inputLines,
                                                         List<EiderParserError> errors)
    {
        Objects.requireNonNull(inputLines);
        Objects.requireNonNull(errors);
        final List<EiderParserRegion> regions = new ArrayList<>();

        boolean continueParsing = true;
        int currentLine = 0;

        while (continueParsing)
        {
            final InputLine inputLine = inputLines.get(currentLine);
            final int start = Math.max(currentLine, backtrackToLast(inputLines, currentLine));
            final int end = moveForwardToEndOfBlock(inputLines, currentLine);
            currentLine = end + 1;
            continueParsing = lastBlockParsed(inputLines, currentLine);
            EiderParserRegionType type = findType(inputLines, start, end);

            if (type != EiderParserRegionType.UNKNOWN)
            {
                regions.add(new EiderParserRegion(start, end, type));
            }
            else
            {
                if (!isAttribute(inputLine.getContents()))
                {
                    //wtf is this?
                    errors.add(new EiderParserError(inputLine.getLineNumber(), 0,
                        "Unknown region type: " + inputLine.getContents(), ParserIssueType.FATAL));
                }
            }
        }

        return regions;
    }

    private static EiderParserRegionType findType(List<InputLine> inputLines, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            final InputLine line = inputLines.get(i);
            if (line.getContents().startsWith(MESSAGE))
            {
                return EiderParserRegionType.MESSAGE;
            }
            else if (line.getContents().startsWith(RESIDENT_DATA))
            {
                return EiderParserRegionType.RESIDENT_DATA;
            }
            else if (line.getContents().startsWith(RECORD))
            {
                return EiderParserRegionType.RECORD;
            }
            else if (line.getContents().startsWith(DEMUXER))
            {
                return EiderParserRegionType.DEMUXER;
            }
            else if (line.getContents().startsWith(EIDER))
            {
                return EiderParserRegionType.EIDER;
            }
            else if (line.getContents().startsWith(ENUM))
            {
                return EiderParserRegionType.ENUM;
            }
        }

        return EiderParserRegionType.UNKNOWN;
    }

    /**
     * Parse an attribute. Assumption: the attribute is a single line, and leading spaces are removed.
     *
     * @param input         the line to parse
     * @param parsingErrors the active list of parsing errors
     * @return an Attribute object
     */
    public static ParsedAttribute parseAttribute(String input, List<EiderParserError> parsingErrors)
    {
        Objects.requireNonNull(parsingErrors);
        if (isAttribute(input))
        {
            if (!input.contains("("))
            {
                //a naked attribute
                return new ParsedAttribute(input.substring(1).trim(), true, Collections.emptyList());
            }

            //extract the name between the @ and the first (
            final String name = input.substring(1, input.indexOf("(")).trim();

            final List<ParsedAttributeItem> attributeItems = new ArrayList<>();

            //extract the parameters between the first ( and the last )
            final String parameters = input.substring(input.indexOf("(") + 1, input.lastIndexOf(")"));

            //split the parameters into a list of strings
            final String[] parametersList = parameters.split(",");

            for (final String parameter : parametersList)
            {
                //split the parameter into a key and a value
                if (parameter.contains("="))
                {
                    final String[] keyValue = parameter.split("=");
                    attributeItems.add(new ParsedAttributeItem(keyValue[0].trim(), keyValue[1].trim()));
                }
                else
                {
                    attributeItems.add(new ParsedAttributeItem(parameter.trim(), null));
                }
            }

            return new ParsedAttribute(name, true, attributeItems);
        }
        else
        {
            return new ParsedAttribute("invalid", false, Collections.emptyList());
        }
    }

    public static ParsedEnum parseEnum(List<InputLine> inputLines, int start, int end,
                                       List<EiderParserError> parsingErrors)
    {
        Objects.requireNonNull(parsingErrors);
        final List<ParsedAttribute> parsedAttributes = new ArrayList<>();

        for (int i = start; i <= end; i++)
        {
            final InputLine inputLine = inputLines.get(i);
            final String inputLineContents = inputLine.getContents();
            if (isAttribute(inputLineContents))
            {
                parsedAttributes.add(parseAttribute(inputLineContents, parsingErrors));
            }
            else if (inputLineContents.startsWith("enum"))
            {
                if (inputLineContents.contains("{"))
                {
                    final String name = inputLineContents.substring(inputLineContents.indexOf("enum") + 4,
                        inputLineContents.indexOf("{")).trim();
                    final List<ParsedEnumItem> enumItemsContents = new ArrayList<>();
                    for (int j = i + 1; j < end; j++)
                    {
                        final String enumItemContents = inputLines.get(j).getContents();
                        final String representation = enumItemContents.substring(enumItemContents.indexOf("=") + 1,
                            enumItemContents.lastIndexOf(";")).trim();
                        final String enumItemName = enumItemContents.substring(0, enumItemContents.indexOf("=")).trim();
                        enumItemsContents.add(new ParsedEnumItem(enumItemName, representation));
                    }
                    return new ParsedEnum(name, enumItemsContents, parsedAttributes);
                }
                else
                {
                    parsingErrors.add(new EiderParserError(inputLine.getLineNumber(), 0,
                        "enum definition invalid. " + "should be 'enum NAME {'", ParserIssueType.FATAL));
                    return null;
                }
            }
            else
            {
                break;
            }
        }

        parsingErrors.add(new EiderParserError(inputLines.get(start).getLineNumber(), 0, "Enum definition invalid",
            ParserIssueType.FATAL));
        return null;
    }

    public static ParsedEiderBlock parseEiderBlock(List<InputLine> inputLines, int start, int end,
                                                   List<EiderParserError> parsingErrors)
    {
        Objects.requireNonNull(parsingErrors);

        for (int i = start; i <= end; i++)
        {
            final InputLine inputLine = inputLines.get(i);
            final String inputLineContents = inputLine.getContents();
            if (inputLineContents.startsWith(EIDER))
            {
                if (inputLineContents.contains("{"))
                {
                    String version = "eider2";
                    String packageName = "undefined";
                    for (int j = i + 1; j < end; j++)
                    {
                        final String currentLine = inputLines.get(j).getContents().trim().toLowerCase();
                        String extractedValue = currentLine.substring(currentLine.indexOf("=") + 1,
                            currentLine.lastIndexOf(";")).trim();

                        if (currentLine.startsWith("version"))
                        {
                            version = extractedValue;
                        }
                        else if (currentLine.startsWith("packagename"))
                        {
                            packageName = extractedValue;
                        }
                    }
                    return new ParsedEiderBlock(packageName, version);
                }
                else
                {
                    parsingErrors.add(new EiderParserError(inputLine.getLineNumber(), 0,
                        "eider block definition invalid. " + "should be 'eider {'", ParserIssueType.FATAL));
                    return null;
                }
            }
            else
            {
                break;
            }
        }

        parsingErrors.add(new EiderParserError(inputLines.get(start).getLineNumber(), 0,
            "Eider block definition invalid", ParserIssueType.FATAL));
        return null;
    }

    public static ParsedRecord parseRecord(List<InputLine> inputLines, int start, int end, List<ParsedEnum> enums,
                                           List<EiderParserError> parsingErrors)
    {
        Objects.requireNonNull(parsingErrors);
        Objects.requireNonNull(enums);
        final List<ParsedAttribute> parsedAttributes = new ArrayList<>();

        for (int i = start; i <= end; i++)
        {
            final InputLine inputLine = inputLines.get(i);
            final String inputLineContents = inputLine.getContents();
            if (isAttribute(inputLineContents))
            {
                parsedAttributes.add(parseAttribute(inputLineContents, parsingErrors));
            }
            else if (inputLineContents.startsWith(RECORD))
            {
                if (inputLineContents.contains("{"))
                {
                    final String name =
                        inputLineContents.substring(inputLineContents.indexOf(RECORD) + RECORD.length(),
                            inputLineContents.indexOf("{")).trim();

                    //extract regions
                    final List<ParsedFieldRegion> regions = extractFieldRegions(inputLines, end, i);

                    //now extract fields by region; records cannot be within records, so don't allow matching of records
                    final List<ParsedField> fields = parseFields(regions, inputLines, parsingErrors, enums, null);

                    return new ParsedRecord(name, parsedAttributes, fields);
                }
                else
                {
                    parsingErrors.add(new EiderParserError(inputLine.getLineNumber(), 0, "record definition invalid. "
                        + "should be 'record NAME {'", ParserIssueType.FATAL));
                    return null;
                }
            }
            else
            {
                break;
            }
        }

        parsingErrors.add(new EiderParserError(inputLines.get(start).getLineNumber(), 0, "Record definition invalid",
            ParserIssueType.FATAL));
        return null;
    }

    private static List<ParsedField> parseFields(List<ParsedFieldRegion> regions,
                                                 List<InputLine> inputLines,
                                                 List<EiderParserError> parsingErrors,
                                                 List<ParsedEnum> enums,
                                                 List<ParsedRecord> records)
    {
        final List<ParsedField> fields = new ArrayList<>();
        for (ParsedFieldRegion region : regions)
        {
            final List<ParsedAttribute> fieldAttributes = new ArrayList<>();
            String fieldName = "";
            String fieldType = "";
            String fieldOrder;
            int fieldOrderInt = -1;
            int failedLine = 0;
            ParsedEnum enumType;
            ParsedRecord recordType;
            ParsedDataType fieldDataType = ParsedDataType.UNKNOWN;
            boolean parseSuccess = false;
            for (int j = region.getStart(); j <= region.getEnd(); j++)
            {
                final String fieldContents = inputLines.get(j).getContents();
                failedLine = inputLines.get(j).getLineNumber();
                if (isAttribute(fieldContents))
                {
                    fieldAttributes.add(parseAttribute(fieldContents, parsingErrors));
                }
                else
                {
                    if (fieldContents.contains(";") && fieldContents.contains("="))
                    {
                        fieldType = fieldContents.substring(0, fieldContents.indexOf(" ")).trim();
                        fieldName = fieldContents.substring(fieldContents.indexOf(" ") + 1,
                            fieldContents.indexOf("=")).trim();
                        fieldOrder = fieldContents.substring(fieldContents.indexOf("=") + 1,
                            fieldContents.indexOf(";")).trim();
                        fieldOrderInt = Integer.parseInt(fieldOrder);
                        fieldDataType = ParsedDataType.fromString(fieldType);
                        if (fieldDataType == ParsedDataType.UNKNOWN)
                        {
                            enumType = attemptEnumMatch(fieldType, enums);
                            if (enumType != null)
                            {
                                fieldDataType = ParsedDataType.ENUM;
                            }
                            else
                            {
                                if (records != null)
                                {
                                    recordType = attemptRecordMatch(fieldType, records);
                                    if (recordType != null)
                                    {
                                        fieldDataType = ParsedDataType.RECORD;
                                    }
                                }
                            }

                        }
                        parseSuccess = true;
                    }
                    else
                    {
                        parseSuccess = false;
                    }
                }
            }

            if (parseSuccess)
            {
                final ParsedField parsedField = new ParsedField(fieldName, fieldType, fieldDataType,
                    fieldOrderInt, fieldAttributes);
                fields.add(parsedField);
            }
            else
            {
                parsingErrors.add(new EiderParserError(failedLine, 0,
                    "Could not parse field", ParserIssueType.WARN));
            }
        }
        return fields;
    }

    public static ParsedMessage parseMessage(List<InputLine> inputLines, int start, int end,
                                             List<ParsedEnum> enums,
                                             List<ParsedRecord> records,
                                             List<EiderParserError> parsingErrors)
    {
        Objects.requireNonNull(parsingErrors);
        Objects.requireNonNull(enums);
        final List<ParsedAttribute> parsedAttributes = new ArrayList<>();

        for (int i = start; i <= end; i++)
        {
            final InputLine inputLine = inputLines.get(i);
            final String inputLineContents = inputLine.getContents();
            if (isAttribute(inputLineContents))
            {
                parsedAttributes.add(parseAttribute(inputLineContents, parsingErrors));
            }
            else if (inputLineContents.startsWith(MESSAGE))
            {
                if (inputLineContents.contains("{"))
                {
                    final String name =
                        inputLineContents.substring(inputLineContents.indexOf(MESSAGE) + MESSAGE.length(),
                            inputLineContents.indexOf("{")).trim();

                    //extract regions
                    final List<ParsedFieldRegion> regions = extractFieldRegions(inputLines, end, i);

                    //extract fields by region
                    final List<ParsedField> fields = parseFields(regions, inputLines, parsingErrors, enums, records);

                    return new ParsedMessage(name, parsedAttributes, fields);
                }
                else
                {
                    parsingErrors.add(new EiderParserError(inputLine.getLineNumber(), 0, "message definition invalid. "
                        + "should be 'message NAME {'", ParserIssueType.FATAL));
                    return null;
                }
            }
            else
            {
                break;
            }
        }

        parsingErrors.add(new EiderParserError(inputLines.get(start).getLineNumber(), 0, "Message definition invalid",
            ParserIssueType.FATAL));
        return null;
    }

    public static ParsedResidentData parseResident(List<InputLine> inputLines, int start, int end,
                                                   List<ParsedEnum> enums,
                                                   List<ParsedRecord> records,
                                                   List<EiderParserError> parsingErrors)
    {
        Objects.requireNonNull(parsingErrors);
        Objects.requireNonNull(enums);
        final List<ParsedAttribute> parsedAttributes = new ArrayList<>();

        for (int i = start; i <= end; i++)
        {
            final InputLine inputLine = inputLines.get(i);
            final String inputLineContents = inputLine.getContents();
            if (isAttribute(inputLineContents))
            {
                parsedAttributes.add(parseAttribute(inputLineContents, parsingErrors));
            }
            else if (inputLineContents.startsWith(RESIDENT_DATA))
            {
                if (inputLineContents.contains("{"))
                {
                    final String name =
                        inputLineContents.substring(inputLineContents.indexOf(RESIDENT_DATA) + RESIDENT_DATA.length(),
                            inputLineContents.indexOf("{")).trim();

                    //extract regions
                    final List<ParsedFieldRegion> regions = extractFieldRegions(inputLines, end, i);

                    //now extract fields by region
                    final List<ParsedField> fields = parseFields(regions, inputLines, parsingErrors, enums, records);

                    return new ParsedResidentData(name, parsedAttributes, fields);
                }
                else
                {
                    parsingErrors.add(new EiderParserError(inputLine.getLineNumber(), 0, "resident definition invalid. "
                        + "should be 'resident NAME {'", ParserIssueType.FATAL));
                    return null;
                }
            }
            else
            {
                break;
            }
        }

        parsingErrors.add(new EiderParserError(inputLines.get(start).getLineNumber(), 0, "resident definition invalid",
            ParserIssueType.FATAL));
        return null;
    }

    /**
     * A region is zero to many attributes followed by a field definition.
     *
     * @param inputLines        the lines to parse
     * @param endOfParentRegion the endOfParentRegion of the parent region
     * @param start             the starting point of the record or field
     * @return a list of regions representing fields & their attributes
     */
    private static List<ParsedFieldRegion> extractFieldRegions(List<InputLine> inputLines, int endOfParentRegion,
                                                               int start)
    {
        final List<ParsedFieldRegion> regions = new ArrayList<>();
        int regionStart = 0;
        for (int j = start + 1; j < endOfParentRegion; j++)
        {
            final String fieldContents = inputLines.get(j).getContents();
            if (fieldContents.startsWith("@"))
            {
                if (regionStart == 0)
                {
                    regionStart = j;
                }
            }
            else
            {
                if (regionStart == 0)
                {
                    regions.add(new ParsedFieldRegion(j, j));
                }
                else
                {
                    regions.add(new ParsedFieldRegion(regionStart, j));
                }
                regionStart = 0;
            }
        }
        return regions;
    }

    /**
     * Attempts to match the given string to an enum type.
     *
     * @param fieldType the field type to match
     * @param enums     the list of enums to match against
     * @return the enum type if found, null otherwise
     */
    private static ParsedEnum attemptEnumMatch(String fieldType, List<ParsedEnum> enums)
    {
        for (ParsedEnum parsedEnum : enums)
        {
            if (parsedEnum.getName().equalsIgnoreCase(fieldType))
            {
                return parsedEnum;
            }
        }
        return null;
    }

    /**
     * Attempts to match the given string to a record type.
     *
     * @param fieldType the field type to match
     * @param records   the list of records to match against
     * @return the record type if found, null otherwise
     */
    private static ParsedRecord attemptRecordMatch(String fieldType, List<ParsedRecord> records)
    {
        for (ParsedRecord parsedRecord : records)
        {
            if (parsedRecord.getName().equalsIgnoreCase(fieldType))
            {
                return parsedRecord;
            }
        }
        return null;
    }
}

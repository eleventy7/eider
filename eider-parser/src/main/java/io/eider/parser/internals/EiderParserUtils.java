package io.eider.parser.internals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.eider.parser.output.EiderParserError;
import io.eider.parser.output.ParserIssueType;

public final class EiderParserUtils
{
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

    public static List<EiderParserRegion> extractRegions(List<InputLine> inputLines, List<EiderParserError> errors)
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
            if (line.getContents().startsWith("message"))
            {
                return EiderParserRegionType.MESSAGE;
            }
            else if (line.getContents().startsWith("repository"))
            {
                return EiderParserRegionType.REPOSITORY;
            }
            else if (line.getContents().startsWith("record"))
            {
                return EiderParserRegionType.RECORD;
            }
            else if (line.getContents().startsWith("enum"))
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

    public static ParsedEnum parseEnum(List<InputLine> inputLines,
                                       int start,
                                       int end,
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
                        final String representation = enumItemContents.substring(enumItemContents.indexOf("(") + 1,
                            enumItemContents.lastIndexOf(")")).trim();
                        final String enumItemName = enumItemContents.substring(0, enumItemContents.indexOf("(")).trim();
                        enumItemsContents.add(new ParsedEnumItem(enumItemName, representation));
                    }
                    return new ParsedEnum(name, enumItemsContents, parsedAttributes);
                }
                else
                {
                    parsingErrors.add(new EiderParserError(inputLine.getLineNumber(), 0, "enum definition invalid. "
                        + "should be 'enum NAME {'", ParserIssueType.FATAL));
                    return null;
                }
            }
            else
            {
                break;
            }
        }

        parsingErrors.add(new EiderParserError(inputLines.get(start).getLineNumber(), 0,
            "Enum definition invalid", ParserIssueType.FATAL));
        return null;
    }
}

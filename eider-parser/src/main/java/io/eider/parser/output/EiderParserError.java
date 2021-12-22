/*
 * Copyright ©2019-2022 Shaun Laurens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.eider.parser.output;

public class EiderParserError
{
    private final int line;
    private final int column;
    private final String message;
    private ParserIssueType type;

    public EiderParserError(final int line, final int column, final String message, final ParserIssueType type)
    {
        this.line = line;
        this.column = column;
        this.message = message;
        this.type = type;
    }

    public int getLine()
    {
        return this.line;
    }

    public int getColumn()
    {
        return column;
    }

    public String getMessage()
    {
        return message;
    }

    public ParserIssueType getType()
    {
        return type;
    }
}

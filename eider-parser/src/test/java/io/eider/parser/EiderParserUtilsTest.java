package io.eider.parser;

import io.eider.parser.internals.EiderParserRegion;
import io.eider.parser.internals.EiderParserRegionType;
import io.eider.parser.internals.InputLine;
import io.eider.parser.internals.ParsedAttribute;
import io.eider.parser.internals.EiderParserUtils;

import io.eider.parser.internals.ParsedEiderBlock;
import io.eider.parser.internals.ParsedEnum;
import io.eider.parser.internals.ParsedMessage;
import io.eider.parser.internals.ParsedRecord;
import io.eider.parser.internals.ParsedResidentData;
import io.eider.parser.output.EiderParserError;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EiderParserUtilsTest
{
    @Test
    void removesCommentsFromInput()
    {
        final String input = "// This is a comment\nthis is a non-comment\n// This is another comment";
        final String expected = "this is a non-comment";

        assertEquals(expected, EiderParserUtils.splitInputIntoUsefulLines(input).get(0).getContents());
    }

    @Test
    void removesLeadingSpacesFromInput()
    {
        final String input = "// This is a comment\n      this is a non-comment\n// This is another comment";
        final String expected = "this is a non-comment";

        assertEquals(expected, EiderParserUtils.splitInputIntoUsefulLines(input).get(0).getContents());
    }

    @Test
    void removesLeadingSpacesFromInput2()
    {
        final String input = """
            // This is a comment
                  this is a non-comment
            // This is another comment";
            """;
        final String expected = "this is a non-comment";

        assertEquals(expected, EiderParserUtils.splitInputIntoUsefulLines(input).get(0).getContents());
    }

    @Test
    void removesBlankLinesFromInput()
    {
        final String input = "// This is a comment\nthis is a non-comment\n\n\n";
        final String expected = "this is a non-comment";

        assertEquals(expected, EiderParserUtils.splitInputIntoUsefulLines(input).get(0).getContents());
    }

    @Test
    void removesCarriageReturnFromInput()
    {
        final String input = "// This is a comment\r\n   this is a non-comment\r\n\r\n\r\n";
        final String expected = "this is a non-comment";

        assertEquals(expected, EiderParserUtils.splitInputIntoUsefulLines(input).get(0).getContents());
    }

    @Test
    void canParseSingleAttribute()
    {
        final String input = "@attribute    (name1  ,    name2 =   value2   )  ";
        final List<EiderParserError> errors = new ArrayList<>();
        final ParsedAttribute attribute = EiderParserUtils.parseAttribute(input, errors);

        assertEquals("attribute", attribute.getName());
        assertTrue(attribute.isValid());
        assertEquals(2, attribute.getAttributeItems().size());
        assertEquals("name1", attribute.getAttributeItems().get(0).getName());
        assertNull(attribute.getAttributeItems().get(0).getValue());
        assertEquals("name2", attribute.getAttributeItems().get(1).getName());
        assertEquals("value2", attribute.getAttributeItems().get(1).getValue());
    }

    @Test
    void canParseNakedAttribute()
    {
        final String input = "@attribute  ";
        final List<EiderParserError> errors = new ArrayList<>();
        final ParsedAttribute attribute = EiderParserUtils.parseAttribute(input, errors);

        assertTrue(attribute.isValid());
        assertEquals("attribute", attribute.getName());
        assertEquals(0, attribute.getAttributeItems().size());
    }

    @Test
    void returnsInvalidIfNotStartingWithAt()
    {
        final String input = "attribute  ";
        final List<EiderParserError> errors = new ArrayList<>();
        final ParsedAttribute attribute = EiderParserUtils.parseAttribute(input, errors);

        assertFalse(attribute.isValid());
    }

    @Test
    void parsesRegionsCorrectly()
    {
        final String input = """
            eider {
                version = 1.0;
                packageName=com.example.eider;
            }
            
            enum PriceType {
            	VALUE1=1;
            	VALUE2=2;
            }
                        
            record OrderBookItem {
            	double price;
            	double size;
            }
                        
            message OrderBook {
            	int16 venue;
            	int16 asset;
            	
            	@repeated(max=16, interface=array)
            	repeated OrderBookItem items;
            }
                        
            @options(snapshotable,difftracking,targetsize=tiny,paged)
            resident Orders {
            	@key(unique=true)
            	int64 key;
            	
            	@index(type=simple_agronahash, relationship=one_to_many)
            	PriceType priceType;
            	
            	@index(type=roaring_fulltext)
            	@fixedlength(65)
            	String searchable;
                        
            	@nullable
            	int64 completedTimestamp;
            }
                   
           demuxer {
            	foo
            	bar
           }
                        
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);

        assertEquals(6, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.EIDER, eiderParserRegions.get(0).getType());
        assertEquals(0, eiderParserRegions.get(0).getStart());
        assertEquals(3, eiderParserRegions.get(0).getEnd());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(1).getType());
        assertEquals(4, eiderParserRegions.get(1).getStart());
        assertEquals(7, eiderParserRegions.get(1).getEnd());
        assertEquals(EiderParserRegionType.RECORD, eiderParserRegions.get(2).getType());
        assertEquals(8, eiderParserRegions.get(2).getStart());
        assertEquals(11, eiderParserRegions.get(2).getEnd());
        assertEquals(EiderParserRegionType.MESSAGE, eiderParserRegions.get(3).getType());
        assertEquals(12, eiderParserRegions.get(3).getStart());
        assertEquals(17, eiderParserRegions.get(3).getEnd());
        assertEquals(EiderParserRegionType.RESIDENT_DATA, eiderParserRegions.get(4).getType());
        assertEquals(18, eiderParserRegions.get(4).getStart());
        assertEquals(29, eiderParserRegions.get(4).getEnd());
        assertEquals(EiderParserRegionType.DEMUXER, eiderParserRegions.get(5).getType());
        assertEquals(30, eiderParserRegions.get(5).getStart());
        assertEquals(33, eiderParserRegions.get(5).getEnd());
    }

    @Test
    void parsesEnumCorrectly()
    {
        final String input = """
            enum PriceType {
            	VALUE1 = 1;
            	VALUE2 =  2  ;
            }
                        
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);


        assertEquals(1, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(0).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);

        assertNotNull(eiderEnum);
        assertEquals("PriceType", eiderEnum.getName());
        assertEquals("VALUE1", eiderEnum.getEnumItemList().get(0).getName());
        assertEquals("1", eiderEnum.getEnumItemList().get(0).getRepresentation());
        assertEquals("VALUE2", eiderEnum.getEnumItemList().get(1).getName());
        assertEquals("2", eiderEnum.getEnumItemList().get(1).getRepresentation());
    }

    @Test
    void parsesEiderBlock()
    {
        final String input = """
            eider {
            	version = eider42;
            	packageName = com.foo.bar;
            }
                        
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);


        assertEquals(1, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.EIDER, eiderParserRegions.get(0).getType());

        final ParsedEiderBlock eiderBlock = EiderParserUtils.parseEiderBlock(inputLines,
            eiderParserRegions.get(0).getStart(), eiderParserRegions.get(0).getEnd(), errors);

        assertNotNull(eiderBlock);
        assertEquals("eider42", eiderBlock.getVersion());
        assertEquals("com.foo.bar", eiderBlock.getPackageName());
    }

    @Test
    void raisesErrorWithInvalidEnumSyntax()
    {
        final String input = """
            enum PriceType 
            {
            	VALUE1=  1;
            	VALUE2  =   2  ;
            }
                        
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);


        assertEquals(1, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(0).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);

        assertNull(eiderEnum);
        assertEquals(1, errors.size());
    }

    @Test
    void raisesErrorWithInvalidInputRegionType()
    {
        final String input = """
            resident PriceType {
            	VALUE1=1;
            	VALUE2=2;
            }
                        
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);


        assertEquals(1, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.RESIDENT_DATA, eiderParserRegions.get(0).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);

        assertNull(eiderEnum);
        assertEquals(1, errors.size());
    }


    @Test
    void parsesRecordWithEnumType()
    {
        final String input = """
           
               enum PriceType {
           	VALUE1 =1;
                              	VALUE2= 2 ;
            }
             
            //a sample record
           record PriceRecord {
                 	double    price = 1;
            double size =2;
            	PriceType priceType=3;
            }
                                         
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);

        assertEquals(2, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(0).getType());
        assertEquals(EiderParserRegionType.RECORD, eiderParserRegions.get(1).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);
        List<ParsedEnum> enums = new ArrayList<>();
        enums.add(eiderEnum);

        final ParsedRecord eiderRecord = EiderParserUtils.parseRecord(inputLines, eiderParserRegions.get(1).getStart(),
            eiderParserRegions.get(1).getEnd(), enums, errors);

        assertNotNull(eiderRecord);
        assertEquals(0, errors.size());
        assertEquals("PriceRecord", eiderRecord.getName());
        assertEquals(3, eiderRecord.getFields().size());
        assertEquals("price", eiderRecord.getFields().get(0).getName());
        assertEquals("size", eiderRecord.getFields().get(1).getName());
        assertEquals(2, eiderRecord.getFields().get(1).getOrder());
        assertEquals("priceType", eiderRecord.getFields().get(2).getName());
    }

    @Test
    void parsesRecordWithAttributes()
    {
        final String input = """
           
           enum PriceType {
           	     VALUE1=1;
                 VALUE2=2;
           }
             
            @something
           record PriceRecord {
                 	double    price =1;
           @index(type=agrona)
            double size =2;
           @index
           @key(a=b)
            	PriceType priceType =3;
            }
                                         
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);

        assertEquals(2, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(0).getType());
        assertEquals(EiderParserRegionType.RECORD, eiderParserRegions.get(1).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);
        List<ParsedEnum> enums = new ArrayList<>();
        enums.add(eiderEnum);

        final ParsedRecord eiderRecord = EiderParserUtils.parseRecord(inputLines, eiderParserRegions.get(1).getStart(),
            eiderParserRegions.get(1).getEnd(), enums, errors);

        assertNotNull(eiderRecord);
        assertEquals(0, errors.size());
        assertEquals("PriceRecord", eiderRecord.getName());
        assertEquals("something", eiderRecord.getAttributes().get(0).getName());
        assertEquals(3, eiderRecord.getFields().size());
        assertEquals("price", eiderRecord.getFields().get(0).getName());
        assertEquals("size", eiderRecord.getFields().get(1).getName());
        assertEquals("index", eiderRecord.getFields().get(1).getAttributes().get(0).getName());
        assertEquals("priceType", eiderRecord.getFields().get(2).getName());
        assertEquals("index", eiderRecord.getFields().get(2).getAttributes().get(0).getName());
        assertEquals("key", eiderRecord.getFields().get(2).getAttributes().get(1).getName());
    }

    @Test
    void parsesMessageWithEnumRecordAndAttributes()
    {
        final String input = """
            enum PriceType {
             VALUE1=1;
             VALUE2=2;
            }
             
            @something
            record PriceRecord {
              double price=1;
           
              @index(type=agrona)
              double size =2;
           
              @index
              @key(a=b)
              PriceType priceType=3;
            }
            
            @foobar
            message PriceMessage {
              PriceRecord priceRecord=1;
              PriceType priceType=2;
              int64 field1=3;
              int64 field2=4;
            }
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);

        assertEquals(3, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(0).getType());
        assertEquals(EiderParserRegionType.RECORD, eiderParserRegions.get(1).getType());
        assertEquals(EiderParserRegionType.MESSAGE, eiderParserRegions.get(2).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);
        List<ParsedEnum> enums = new ArrayList<>();
        enums.add(eiderEnum);

        final ParsedRecord eiderRecord = EiderParserUtils.parseRecord(inputLines, eiderParserRegions.get(1).getStart(),
            eiderParserRegions.get(1).getEnd(), enums, errors);
        List<ParsedRecord> records = new ArrayList<>();
        records.add(eiderRecord);

        final ParsedMessage eiderMessage = EiderParserUtils.parseMessage(inputLines,
            eiderParserRegions.get(2).getStart(), eiderParserRegions.get(2).getEnd(), enums, records, errors);

        assertNotNull(eiderMessage);
        assertEquals(0, errors.size());
        assertEquals("PriceMessage", eiderMessage.getName());
        assertEquals("foobar", eiderMessage.getAttributes().get(0).getName());
        assertEquals(4, eiderMessage.getFields().size());
        assertEquals("priceRecord", eiderMessage.getFields().get(0).getName());
        assertEquals("PriceRecord", eiderMessage.getFields().get(0).getTypeString());
        assertEquals("priceType", eiderMessage.getFields().get(1).getName());
        assertEquals("PriceType", eiderMessage.getFields().get(1).getTypeString());
        assertEquals("field1", eiderMessage.getFields().get(2).getName());
        assertEquals("int64", eiderMessage.getFields().get(2).getTypeString());
        assertEquals("field2", eiderMessage.getFields().get(3).getName());
        assertEquals("int64", eiderMessage.getFields().get(3).getTypeString());
    }

    @Test
    void parsesResidentDataWithEnumRecordAndAttributes()
    {
        final String input = """
            enum PriceType {
             VALUE1=1;
             VALUE2=2;
            }
             
            @something
            record PriceRecord {
              double price=1;
           
              @index(type=agrona)
              double size =2;
           
              @index
              @key(a=b)
              PriceType priceType=3;
            }
            
            @foobar
            resident PriceDataStore {
              PriceRecord priceRecord=1;
              PriceType priceType=2;
              int64 field1=3;
              int64 field2=4;
            }
            """;
        final List<EiderParserError> errors = new ArrayList<>();
        final List<InputLine> inputLines = EiderParserUtils.splitInputIntoUsefulLines(input);
        List<EiderParserRegion> eiderParserRegions = EiderParserUtils.extractRegions(inputLines, errors);

        assertEquals(3, eiderParserRegions.size());
        assertEquals(EiderParserRegionType.ENUM, eiderParserRegions.get(0).getType());
        assertEquals(EiderParserRegionType.RECORD, eiderParserRegions.get(1).getType());
        assertEquals(EiderParserRegionType.RESIDENT_DATA, eiderParserRegions.get(2).getType());

        final ParsedEnum eiderEnum = EiderParserUtils.parseEnum(inputLines, eiderParserRegions.get(0).getStart(),
            eiderParserRegions.get(0).getEnd(), errors);
        List<ParsedEnum> enums = new ArrayList<>();
        enums.add(eiderEnum);

        final ParsedRecord eiderRecord = EiderParserUtils.parseRecord(inputLines, eiderParserRegions.get(1).getStart(),
            eiderParserRegions.get(1).getEnd(), enums, errors);
        List<ParsedRecord> records = new ArrayList<>();
        records.add(eiderRecord);

        final ParsedResidentData eiderResidentData = EiderParserUtils.parseResident(inputLines,
            eiderParserRegions.get(2).getStart(), eiderParserRegions.get(2).getEnd(), enums, records, errors);

        assertNotNull(eiderResidentData);
        assertEquals(0, errors.size());
        assertEquals("PriceDataStore", eiderResidentData.getName());
        assertEquals("foobar", eiderResidentData.getAttributes().get(0).getName());
        assertEquals(4, eiderResidentData.getFields().size());
        assertEquals("priceRecord", eiderResidentData.getFields().get(0).getName());
        assertEquals("PriceRecord", eiderResidentData.getFields().get(0).getTypeString());
        assertEquals("priceType", eiderResidentData.getFields().get(1).getName());
        assertEquals("PriceType", eiderResidentData.getFields().get(1).getTypeString());
        assertEquals("field1", eiderResidentData.getFields().get(2).getName());
        assertEquals("int64", eiderResidentData.getFields().get(2).getTypeString());
        assertEquals("field2", eiderResidentData.getFields().get(3).getName());
        assertEquals("int64", eiderResidentData.getFields().get(3).getTypeString());
    }
}

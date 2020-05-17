# Eider

![Java CI](https://github.com/eleventy7/eider/workflows/Java%20CI/badge.svg) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/eleventy7/eider.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/eleventy7/eider/context:java)

Work in progress multi-target annotation based flyweight generator.

Given an specification object, Eider generates a flyweight that can be used to read and write to a buffer. The original specification object is not used at runtime, only for defining the layout of the buffer. The generated flyweight has no runtime dependencies beyond Java and the targetted buffer implementation.

Initial implementation uses Agrona Mutable Direct Buffers for read and write. Random access writes and reads are supported, although sequential reads/writes will provide higher performance. 

Initial release has some severe type restrictions:

- boolean
- int
- long
- fixed length strings

Planned for future releases:

- variable length fields - this will come at the cost of a header providing structural data to the reader so that random reads remain possible.
- JEP 370, JEP 383 and Intel PCJ (https://github.com/pmem/pcj) implementations 
- repeating groups and sub-objects
- Nullable objects with customizable null representations 
- schema validation
- allocation free header validation
- segmented reader/writer (i.e. a single object which contains multiple Eider objects read/written into a single buffer)

Features not planned for future releases:

- versioning or backwards/forwards compatibility
- support for anything but JVM
- migration

### Sample

The following specification defines an object with an identifier, timestamp, enabled flag and a 9 character CUSIP string. The eiderId here is the message type identifier, and is written to the buffer at a fixed position. If no predefined value is given, one is automatically assigned and made available as a static field on the generated object. The eider Id can be peeked to simplify building demuxers. 

```java
@EiderSpec(eiderId = 42)
public class SampleImplementation
{
    private int id;
    private long timestamp;
    private boolean enabled;
    @EiderAttribute(maxLength = 9)
    private String cusip;
}
```

Using the generated object is as simple as:

```java
final SampleImplementationEider read = new SampleImplementationEider();
final SampleImplementationEider write = new SampleImplementationEider();
final EpochClock clock = new SystemEpochClock();
final long now = clock.time();
final int initialOffset = 0;

ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SampleImplementationEider.BUFFER_LENGTH);

write.setWriteBuffer(buffer, initialOffset);
write.writeHeader();
write.writeCusip("037833100");
write.writeEnabled(true);
write.writeId(213);
write.writeTimestamp(now);

read.setReadBuffer(buffer, initialOffset);
Assertions.assertTrue(read.validateHeader());
Assertions.assertEquals("037833100", read.readCusip());
Assertions.assertTrue(read.readEnabled());
Assertions.assertEquals(now, read.readTimestamp());
Assertions.assertEquals(213, read.readId());
```

### Where is this useful?

It's primarily being used for another project with messages sent over Aeron and Aeron Cluster byte buffers. The intent behind the PJC variant is to investigate Aeron Cluster using persistent memory for the state machine internal data as well as snapshotting to persistent memory.

### Requirements

- Java 14 (to enable work on the JEP 370 variant)
- Gradle 6.4.1

### Why Eider?

Eider ducks produce some of the highest performance and fluffiest down available. Compared to some other flyweight generators, Eider intends to be soft and fluffy to use, while retaining high performance.
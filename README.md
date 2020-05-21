# Eider

![Java CI](https://github.com/eleventy7/eider/workflows/Java%20CI/badge.svg) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/eleventy7/eider.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/eleventy7/eider/context:java)

Annotation based flyweight generator. This is not currently used in production.

Given a specification object, Eider generates a flyweight that can be used to read and write to a buffer with random access. The original specification object is not used at runtime. The generated flyweight has no runtime dependencies beyond Java and the targetted buffer implementation.

Initial implementation uses Agrona Mutable Direct Buffers for read and write. Values are read from and written to the buffer directly - they do not hold any field values internally - and as a result, cannot be used or held like regular objects. Random access writes and reads are supported, although sequential reads/writes will provide higher performance. 

Current features:

- type support
    - boolean
    - int
    - long
    - fixed length ASCII strings
- generate flyweights that support fixed length objects
- flyweight backed sequence generator
- fixed size repositories
- optional transactional support on each flyweight. If this is enabled, the flyweight adds `beginTransaction`, `commit` and `rollback` methods. Note, by default reads are dirty; the buffer is only rolled back to the state it was in when `beginTransaction` was called if `rollback` was called. This feature is intended to support repositories within an Aeron Cluster hosted replicated state machine. Note: this will allocate a buffer of length equal to the flyweight buffer length internally.   

Planned for near future:

- segmented reader/writer (i.e. a single object which contains multiple Eider objects read/written into a single buffer)

Features that might make it into future versions:

- transaction support in the repositories
- schema validation
- JEP 370, JEP 383 and Intel PCJ (https://github.com/pmem/pcj) implementations
- single variable length string or byte[] field
- byte[], BigDecimal, char, short types support
- mulitple variable length fields - this will come at the cost of a header providing structural data to the reader so that random reads remain possible.
- repeating groups and sub-objects
- Nullable objects with customizable null representations 

Features not planned for future releases:

- versioning or backwards/forwards compatibility
- support for anything but JVM
- thread safety
- migrations

### Flyweight Sample

The following specification defines an object with an identifier, timestamp, enabled flag and a 9 character CUSIP string. The eiderId here is the message type identifier, and is written to the buffer at a fixed position regardless of message specification. If no predefined value is given, one is automatically assigned and made available as a static field on the generated object. The eider Id can be used to simplify building demuxers. 

```java
@EiderSpec(eiderId = 42)
public class Sample
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
final Sample read = new Sample();
final Sample write = new Sample();
final EpochClock clock = new SystemEpochClock();
final long now = clock.time();
final int initialOffset = 0;

ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(Sample.BUFFER_LENGTH);

write.setUnderlyingBuffer(buffer, initialOffset);
read.setUnderlyingBuffer(buffer, initialOffset);

write.writeHeader();
write.writeCusip("037833100");
write.writeEnabled(true);
write.writeId(213);
write.writeTimestamp(now);

Assertions.assertTrue(read.validateHeader());
Assertions.assertEquals("037833100", read.readCusip());
Assertions.assertTrue(read.readEnabled());
Assertions.assertEquals(now, read.readTimestamp());
Assertions.assertEquals(213, read.readId());
```

### Transactional Flyweight Sample

Flyweights may optionally be transactional. Note that this will allocate a buffer of length `Flyweight.BUFFER_LENGTH` when the flyweight is created. This internal transactional buffer will be reused as the flyweight moves around a buffer.

```java
@EiderSpec(transactional = true)
public class SampleTxn
{
    @EiderAttribute(unique = true, key = true)
    private int id;
    @EiderAttribute(maxLength = 9, indexed = true)
    private String cusip;
}
```

Setting `transactional = true` on the `@EiderSpec` attribute enables transaction support. Note that reads before commit/rollback are dirty.

```java
final String CUSIP = "037833100";
final SampleTxn read = new SampleTxn();
final SampleTxn write = new SampleTxn();

ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SampleTxn.BUFFER_LENGTH);

read.setUnderlyingBuffer(buffer, 0);
write.setUnderlyingBuffer(buffer, 0);

write.writeCusip(CUSIP);
Assertions.assertEquals(CUSIP, read.readCusip());

write.beginTransaction();
write.writeCusip("zzzzzzzzz");
//by default dirty reads are supported
Assertions.assertEquals("zzzzzzzzz", read.readCusip());
write.rollback();

Assertions.assertEquals(CUSIP, read.readCusip());
```

Warnings: 

- any call to `setUnderlyingBuffer` will invalidate the transaction buffer. You cannot call `beginTransaction` followed by `setUnderlyingBuffer` followed by `rollback` for example
- you cannot call `rollback` until you have called `beginTransaction`
- you cannot call `commit` after calling `rollback`

### Sequences

```java
@EiderSpec(name = "SequenceGenerator")
public class SequenceGeneratorSpec
{
    @EiderAttribute(sequence = true)
    private int tradeId;
}
```

One spec can hold more than one sequence. Initialize and get new sequences by:

```java
final SequenceGenerator generator = new SequenceGenerator();
ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(SequenceGenerator.BUFFER_LENGTH);
generator.setUnderlyingBuffer(buffer, 0);

//initialize to 1
generator.writeTradeId(1);

//get next tradeId
int nextTrade = generator.nextTradeIdSequence();
```


### Repository Sample

Repositories do not yet support transactions.

```java
@EiderRepository
@EiderSpec
public class SampleSpec
{
    @EiderAttribute(unique = true, key = true)
    private int id;
    @EiderAttribute(maxLength = 9, indexed = true)
    private String cusip;
}
```

To enable repository generation, just add a `@EiderRepository` attribute to a `@EiderSpec` object. You will have to set exactly one field to be a key using `@EiderAttribute(unique = true, key = true)`.

The repository generated will allow you to work with fixed size repositories that cannot hold more items than specified.

```java
final SampleSpecRepository repository = SampleSpecRepository.createWithCapacity(2);

SampleSpec flyweight = repository.createWithKey(1);
flyweight.writeCusip("CUSIP0001");
flyweight = repository.createWithKey(2);
flyweight.writeCusip("CUSIP0002");

flyweight = repository.getByKey(1);
Assertions.assertEquals("CUSIP0001", flyweight.readCusip());
flyweight = repository.getByKey(2);
Assertions.assertEquals("CUSIP0002", flyweight.readCusip());
```

Warnings:

- You will not be able to alter the key of a flyweight returned by the repository using `createWithKey` or `getByKey`.

### Where is this useful?

It's primarily being used for another project with messages sent over Aeron and Aeron Cluster byte buffers, plus data held within a Replicated State Machine running within Aeron Cluster. 
 
The intent behind the PJC variant is to investigate Aeron Cluster using persistent memory for the state machine internal data as well as snapshotting to persistent memory.

### Requirements

- Java 11
- Gradle 6.4.1

### Why Eider?

Eider ducks produce some of the highest performance and fluffiest down available. Compared to some other flyweight generators, Eider intends to be soft and fluffy to use, while retaining high performance.
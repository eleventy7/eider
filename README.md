# Eider

### Eider is not suitable for production usage, and further development on Eider has ended.

![Java CI](https://github.com/eleventy7/eider/workflows/Java%20CI/badge.svg) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/eleventy7/eider.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/eleventy7/eider/context:java) [![Known Vulnerabilities](https://snyk.io/test/github/eleventy7/eider/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/eleventy7/eider?targetFile=build.gradle)

Annotation based flyweight generator built for fast iteration while developing [Aeron Cookbook](https://aeroncookbook.com). Intended for messages over Aeron and Aeron IPC when all processes are built and deployed as a single unit. Only suitable for single threaded usage scenarios. 

Given a specification object, Eider generates a zero-copy flyweight that can be used to read and write to a buffer with random access. The original specification object is not used at runtime. The generated flyweight has no runtime dependencies beyond Java and the target buffer implementation.

Initial implementation uses Agrona Direct Buffers for read and requires Mutable Direct Buffers for write support. Values are read from and written to the buffer directly - they do not hold any field values internally - and as a result, cannot be used or held like regular objects. Random access writes and reads are supported. 

Current features:

- Generate zero-copy flyweights that support fixed length objects
    - supports multiple underlying buffers including `UnsafeBuffer`, `MutableDirectBuffer` and `DirectBuffer` implementations. Object adjusts internally depending on the provided buffer implementation, making it simpler to work with in read only paths such as Aeron `Subscriptions` and `EgressListeners`.
    - Type support is limited to:
        - double  
        - boolean
        - short
        - int
        - long
        - fixed length ASCII strings. Use `WithPadding` variants to write with padding (this is useful for reused flyweights). In all cases, trailing space chars get trimmed on read.
- Optional Header generation plus a helper to detect message types in a buffer
    - see Aeron Cookbook for [sample in use](https://github.com/eleventy7/aeron-cookbook-code/blob/master/cluster-core/src/main/java/com/aeroncookbook/cluster/rsm/node/RsmDemuxer.java)
    - headers can be turned off for use with Agrona RingBuffer implementations which has a built-in message type id header.
- Sequence Generator
- Optional repositories
    - Note! Repositories hold data within internal buffers and maps. This will cause allocation.
    - repositories hold a maximum number of items, and cannot be resized at runtime
    - `appendWithKey` appends an item to the end of the buffer, up to the pre-defined capacity
    - `getByKey`, `getByBufferIndex`, `getByBufferOffset`, `containsKey` and `Iterator<>` functionality
    - `getCrc32` useful to support cross process comparison of repository contents (e.g. in a Aeron Cluster determinism check)
    - `getOffsetByBufferIndex` and `appendByCopyFromBuffer` which can be used for Aeron Cluster snapshot reading and writing.
    - optional indexed fields. Indexes support updates and transactions. Indexes update synchronously at the time a field write occurs. 
    - optional Unique indexes on fields
    - optional transactional support.  
- Optional transactional support on each flyweight. If this is enabled, the flyweight adds `beginTransaction`, `commit` and `rollback` methods. Note, by default reads are dirty; the buffer is only rolled back to the state it was in when `beginTransaction` was called if `rollback` was called. 
    - Note: this will allocate a buffer of length equal to the flyweight buffer length internally.   
- Composite reader/writer
    - provides a single, keyed object which contains multiple Eider objects read/written into a single buffer
    - Optional repository support
- Reasonable performance
    - Performance varies on the object complexity and buffer implementation. JMH test included uses Agrona UnsafeBuffer.
    - Complex transactional objects read and write around 1.2 million messages/second (a roundtrip of around 0.82μs) 
    - Simple non-transactional objects read and write at around 175 million messages/second (a roundtrip of around 5.7ns)

Features not planned for future releases:

- byte[], BigDecimal, char or other type support
- versioning or backwards/forwards compatibility
- support for anything but JVM
- thread safety
- schema validation
- migrations
- multiple variable length fields
- more than one repeating group per message
- Nullable objects with customizable null representations 
- Maven Central publishing

### Flyweight Sample

The following specification defines an object with an identifier, timestamp, enabled flag and a 9 character CUSIP string. The eiderId here is the message type identifier, and is written to the buffer at a fixed position regardless of message specification. If no predefined value is given, one is automatically assigned and made available as a static field on the generated object. The eider Id can be used to simplify building demuxers. 

```java
@EiderSpec(eiderId = 42, name="Sample")
public class SampleSpec
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
    @EiderAttribute(key = true)
    private int id;
    @EiderAttribute(maxLength = 9)
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
final SequenceGenerator generator = SequenceGenerator.INSTANCE();

//initialize to 1
generator.initializeTradeId(1);

//get next tradeId
int nextTrade = generator.nextTradeIdSequence();
```

### Repeating Groups

Warning: support is basic at this time, and is focused on a use case involving Agrona RingBuffer `tryClaim`.

```java
@EiderRepeatableRecord
public class OrderBookItem
{
    private long price;
    private long size;
}

@EiderSpec(eiderId = 111, name = "OrderBook", header = false)
public class OrderBookSpec
{
    private short pair;
    private short venue;
    @EiderAttribute(repeatedRecord = true)
    private OrderBookItem items;
}
```

A repeating group can be specified with the `@EiderRepeatableRecord` annotation. Some notes:

- repeating group messages are not fixed length. To get the length of the repeating group, use the generated `precomputeBufferLength` method, specifying how many records are to be included.
- the `@EiderAttribute` annotation must be on object to be the repeating group, plus `@EiderAttribute(repeatedRecord = true)` on the field to be repeated.
- some features are not supported with repeating groups, notably Repositories, Transactional and Indexed fields.
- resizing a pre-existing repeating group may leave garbage in the buffer. The initial use case is to use this with Agrona/Aeron tryClaim functionality, which does not result in buffer reuse.

Writing the repeating group:

```java
OrderBook entry = new OrderBook();
ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(entry.precomputeBufferLength(10));
entry.setUnderlyingBuffer(buffer, 0);
entry.resetOrderBookItemSize(10); //sets the max size to 10; performs a buffer limit check
entry.writePair((short)1);
entry.writeVenue((short)2);

for (int i = 0; i < 10; i++)
{
    entry.getOrderBookItem(i).writePrice(i * 100);
    entry.getOrderBookItem(i).writeSize(i * 1000);
}
```

Reading the repeating group:

```java
OrderBook reader = new OrderBook();
reader.setUnderlyingBuffer(buffer, 0);
reader.readOrderBookItemSize(); //read the size of the repeating group from the dedicated size field in the buffer
assertEquals((short)1, reader.readPair());
assertEquals((short)2, reader.readVenue());

for (int i = 0; i < 10; i++)
{
    assertEquals(i * 100, reader.getOrderBookItem(i).readPrice());
    assertEquals(i * 1000, reader.getOrderBookItem(i).readSize());
}
```

### Repository Sample

```java
@EiderRepository(transactional = true)
@EiderSpec
public class SampleSpec
{
    @EiderAttribute(key = true)
    private int id;
    @EiderAttribute(maxLength = 9)
    private String cusip;
}
```

To enable repository generation, just add a `@EiderRepository` attribute to a `@EiderSpec` object. You will have to set exactly one field to be a key using `@EiderAttribute(key = true)`.

The repository generated will allow you to work with fixed size repositories that cannot hold more items than specified.

```java
final SampleSpecRepository repository = SampleSpecRepository.createWithCapacity(2);

SampleSpec flyweight = repository.createWithKey(1);
flyweight.writeCusip("CUSIP0001");

long crc32Initial = repository.getCrc32();

repository.beginTransaction();

flyweight = repository.createWithKey(2);
flyweight.writeCusip("CUSIP0002");
long crc32Final = repository.getCrc32();

flyweight = repository.getByKey(1);
Assertions.assertEquals("CUSIP0001", flyweight.readCusip());
flyweight = repository.getByKey(2);
Assertions.assertEquals("CUSIP0002", flyweight.readCusip());
Assertions.assertNotEquals(crc32Initial, crc32Final);

repository.rollback();

Assertions.assertNull(repository.getByKey(2));
Assertions.assertEquals(crc32Initial, repository.getCrc32());
```

Repositories also hold an iterator, which allows you to iterate through all elements in the buffer.

Warnings:

- You will not be able to alter the key of a flyweight returned by the repository using `createWithKey` or `getByKey`.

### Indexed Repository

```java
@EiderRepository(transactional = true)
@EiderSpec
public class SampleSpec
{
    @EiderAttribute(key = true)
    private int id;
    @EiderAttribute(maxLength = 9, indexed = true)
    private String cusip;
}
```

Indexes are added when an `@EiderRepository` object has `@EiderAttribute` with `indexed = true`.

```java
final SampleSpecRepository repository = SampleSpecRepository.createWithCapacity(2);

SampleSpec flyweight = repository.createWithKey(1);
flyweight.writeCusip("CUSIP0001");
flyweight = repository.createWithKey(2);
flyweight.writeCusip("CUSIP0002");

List<Integer> allCusip1 = repository.getAllWithIndexCusipValue("CUSIP0001");
flyweight = repository.getByOffset(allCusip1.get(0));

Assertions.assertEquals(1, allCusip1.size());
Assertions.assertEquals("CUSIP0001", flyweight.readCusip());

flyweight.writeCusip("CUSIP0003");

List<Integer> allCusip1NowEmpty = repository.getAllWithIndexCusipValue("CUSIP0001");
List<Integer> allCusip3 = repository.getAllWithIndexCusipValue("CUSIP0003");
Assertions.assertEquals(0, allCusip1NowEmpty.size());
Assertions.assertEquals(1, allCusip3.size());
```

### Repository support for snapshotting

```java
final EiderObjectRepository sourceRepo = EiderObjectRepository.createWithCapacity(3);
final EiderObjectRepository destRepo = EiderObjectRepository.createWithCapacity(3);

//...write some data to the sourceRepo...

for (int i = 0; i < sourceRepo.getCurrentCount(); i++)
{
    int offset = sourceRepo.getOffsetByBufferIndex(i);
    destRepo.appendByCopyFromBuffer(sourceRepo.getUnderlyingBuffer(), offset);
}

assertEquals(sourceRepo.getCrc32(), destRepo.getCrc32());
```

When writing to an Aeron Cluster snapshot, use the `getOffsetByBufferIndex` to step through the buffer. When reading from the Aeron Cluster snapshot image, make use of `appendByCopyFromBuffer` to append an item into the repository by copying from the provided image.

### Composite Sample

Composite objects allow you to construct a buffer containing 2..n `EiderSpec` objects. 

Given the following objects:

```java
@EiderSpec(name = "OrderStatus")
public class OrderStatusSpec
{
    private boolean filled;
    private long acceptedTimestamp;
    private long filledTimestamp;
}

@EiderSpec(name = "Order")
public class OrderSpec
{
    @EiderAttribute(maxLength = 14)
    private String clOrdId;
    @EiderAttribute(maxLength = 1)
    private String side;
    private long price;
    private long quantity;
    @EiderAttribute(maxLength = 9)
    private String instrument;
}
```
We can construct a composite by:

```java
@EiderComposite(name = "OrderBookEntry", eiderId = 688)
@EiderRepository
public class OrderBookEntrySpec
{
    @EiderAttribute(key = true)
    long id;
    OrderSpec order;
    OrderStatusSpec status;
}
```

This will produce a composite object that can use an internal or external buffer. For example:

```java
ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(OrderBookEntry.BUFFER_LENGTH);

OrderBookEntry entry = new OrderBookEntry(buffer, 0);
entry.getOrder().writeClOrdId("Order123");
entry.getOrder().writePrice(12300L);
entry.getOrder().writeQuantity(1_000_000L);
entry.getStatus().writeAcceptedTimestamp(500L);
entry.getStatus().writeFilledTimestamp(800L);

Assertions.assertEquals("Order123", entry.getOrder().readClOrdId());
Assertions.assertEquals(12300L, entry.getOrder().readPrice());
Assertions.assertEquals(1_000_000L, entry.getOrder().readQuantity());
Assertions.assertEquals(500L, entry.getStatus().readAcceptedTimestamp());
Assertions.assertEquals(800L, entry.getStatus().readFilledTimestamp());

short eiderSpecId = EiderHelper.getEiderSpecId(0, buffer);
Assertions.assertEquals(688, eiderSpecId);
```

Composite Repositories are supported as well. They behave much the same as object repositories.

```java
OrderBookEntryRepository repository = OrderBookEntryRepository.createWithCapacity(10);
OrderBookEntry entry = repository.appendWithKey(1);
...
boolean containsIt = repository.containsKey(1);
...
OrderBookEntry read = repository.getByKey(1);
...
while (repository.allItems().hasNext())
{
    OrderBookEntry item = repository.allItems().next();
    ...
}
```

### Where is this useful?

It's primarily being used for another project with messages sent over Aeron and Aeron Cluster byte buffers, plus data held within a Replicated State Machine running within Aeron Cluster. 
 
### Requirements

- Java 17
- Gradle 7.3 
- Agrona 1.14.0

/*
 * Copyright 2019-2020 Shaun Laurens.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.eider.processor;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Int2IntHashMap;
import org.agrona.concurrent.UnsafeBuffer;

public class AgronaWriter implements EiderCodeWriter
{
    private static final String BUFFER = "buffer";
    private static final String MUTABLE_BUFFER = "mutableBuffer";
    private static final String UNSAFE_BUFFER = "unsafeBuffer";
    private static final String JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN = "java.nio.ByteOrder.LITTLE_ENDIAN)";
    private static final String JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1 = ", java.nio.ByteOrder.LITTLE_ENDIAN)";
    private static final String TRUE = "true";
    private static final String OFFSET = "offset";
    private static final String RETURN_TRUE = "return true";
    private static final String RETURN_FALSE = "return false";
    private static final String TRANSACTION_COPY_BUFFER_SET_FALSE = "transactionCopyBufferSet = false";
    private static final String FLYWEIGHT = "_FLYWEIGHT";
    private static final String WRITE = "write";
    private static final String JAVA_UTIL = "java.util";
    private static final String ITERATOR = "Iterator";
    private static final String UNFILTERED_ITERATOR = "UnfilteredIterator";
    private static final String RETURN_FLYWEIGHT = "return flyweight";
    private static final String THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION =
        "throw new java.util.NoSuchElementException()";
    private static final String BUFFER_LENGTH_1 = ".BUFFER_LENGTH + 1";
    private static final String INTERNAL_BUFFER = "internalBuffer";
    private static final String CAPACITY = "capacity";
    private static final String RETURN_NULL = "return null";

    @Override
    public void generate(final ProcessingEnvironment pe,
                         final List<PreprocessedEiderObject> forObjects,
                         final List<PreprocessedEiderComposite> composites)
    {
        String packageName = null;

        final AgronaWriterGlobalState globalState = new AgronaWriterGlobalState();

        for (final PreprocessedEiderObject object : forObjects)
        {
            packageName = object.getPackageNameGen();
            AgronaWriterState state = new AgronaWriterState();
            generateSpecObject(pe, object, state, globalState);
            if (object.buildRepository())
            {
                generateSpecRepository(pe, object);
            }
        }

        if (packageName != null)
        {
            for (final PreprocessedEiderComposite composite : composites)
            {
                generateComposite(pe, composite, globalState);
                if (composite.buildRepository())
                {
                    generateCompositeRepository(pe, composite);
                }
            }
            generateStandardFiles(pe, "io.eider.Helper");
        }
    }

    private void generateCompositeRepository(final ProcessingEnvironment pe,
                                             final PreprocessedEiderComposite composite)
    {
        String keyField = composite.getKeyName();

        if (keyField == null)
        {
            throw new RuntimeException("Repository objects must have exactly one key field");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(composite.getRepositoryName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethods(buildCompositeRepositoryMethods(composite))
            .addFields(buildCompositeRepositoryFields(composite))
            .addTypes(buildCompositeRepositoryIterators(composite));

        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(composite.getPackageNameGen(), generated)
            .addFileComment("AGRONA COMPOSITE REPOSITORY GENERATED BY EIDER AT "
                + LocalDateTime.now(ZoneId.of("UTC")).toString()
                + "Z. SPEC:")
            .addFileComment(composite.getClassNameInput())
            .addFileComment(". DO NOT MODIFY.")
            .build();

        try
        { // write the file
            JavaFileObject source = pe.getFiler()
                .createSourceFile(composite.getPackageNameGen() + "." + composite.getRepositoryName());
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file already existing after its first run, this is normal
        }
    }

    private Iterable<TypeSpec> buildCompositeRepositoryIterators(PreprocessedEiderComposite composite)
    {
        List<TypeSpec> results = new ArrayList<>();

        final ClassName iterator = ClassName.get(JAVA_UTIL, ITERATOR);
        final ClassName genObj = ClassName.get("", composite.getName());
        final TypeName iteratorGen = ParameterizedTypeName.get(iterator, genObj);

        TypeSpec allItems = TypeSpec.classBuilder(UNFILTERED_ITERATOR)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .addSuperinterface(iteratorGen)
            .addField(FieldSpec.builder(ClassName.get(composite.getPackageNameGen(), composite.getName()),
                "iteratorFlyweight", Modifier.PRIVATE).initializer("new " + composite.getName() + "()").build())
            .addField(FieldSpec.builder(int.class, "currentOffset", Modifier.PRIVATE).initializer("0").build())
            .addMethod(buildCompositeAllIteratorHasNext(composite))
            .addMethod(buildCompositeAllIteratorNext(composite))
            .addMethod(buildCompositeAllIteratorReset())
            .build();

        results.add(allItems);

        return results;
    }

    private MethodSpec buildCompositeAllIteratorReset()
    {
        return MethodSpec.methodBuilder("reset")
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get("", UNFILTERED_ITERATOR))
            .addStatement("currentOffset = 0")
            .addStatement("return this")
            .build();
    }

    private MethodSpec buildCompositeAllIteratorNext(PreprocessedEiderComposite composite)
    {
        return MethodSpec.methodBuilder("next")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(composite.getPackageNameGen(), composite.getName()))
            .beginControlFlow("if (hasNext())")
            .beginControlFlow("if (currentOffset > maxUsedOffset)")
            .addStatement(THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION)
            .endControlFlow()
            .addStatement("iteratorFlyweight.setUnderlyingBuffer(internalBuffer, currentOffset)")
            .addStatement("currentOffset = currentOffset + " + composite.getName() + BUFFER_LENGTH_1)
            .addStatement("return iteratorFlyweight")
            .endControlFlow()
            .addStatement(THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION)
            .build();
    }

    private MethodSpec buildCompositeAllIteratorHasNext(PreprocessedEiderComposite composite)
    {
        return MethodSpec.methodBuilder("hasNext")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addStatement("return currentCount != 0 && (currentOffset + "
                +
                composite.getName()
                +
                ".BUFFER_LENGTH + 1 <=maxUsedOffset)")
            .build();
    }

    private Iterable<FieldSpec> buildCompositeRepositoryFields(PreprocessedEiderComposite composite)
    {
        List<FieldSpec> results = new ArrayList<>();
        results.add(FieldSpec
            .builder(ExpandableDirectByteBuffer.class, INTERNAL_BUFFER)
            .addJavadoc("The internal MutableDirectBuffer holding capacity instances.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(Int2IntHashMap.class, "offsetByKey")
            .addJavadoc("For mapping the key to the offset.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(int.class, "maxUsedOffset")
            .addJavadoc("The current max offset used of the buffer.")
            .initializer("0")
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(int.class, "currentCount")
            .addJavadoc("The current count of elements in the underlying buffer.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("0")
            .build());

        results.add(FieldSpec
            .builder(int.class, "maxCapacity")
            .addJavadoc("The maximum count of elements in the buffer.")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build());

        results.add(FieldSpec
            .builder(ClassName.get("", UNFILTERED_ITERATOR), "unfilteredIterator")
            .addJavadoc("The iterator for unfiltered items.")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build());

        results.add(FieldSpec
            .builder(int.class, "repositoryBufferLength")
            .addJavadoc("The length of the internal buffer.")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build());

        results.add(FieldSpec.builder(ClassName.get(composite.getPackageNameGen(), composite.getName()), "flyweight")
            .addJavadoc("The flyweight used by the repository.")
            .initializer("null")
            .addModifiers(Modifier.PRIVATE)
            .build());

        return results;
    }

    private Iterable<MethodSpec> buildCompositeRepositoryMethods(PreprocessedEiderComposite composite)
    {
        List<MethodSpec> results = new ArrayList<>();
        results.add(
            MethodSpec.constructorBuilder()
                .addJavadoc("Standard constructor.")
                .addParameter(
                    ParameterSpec.builder(int.class, CAPACITY)
                        .addJavadoc("capacity to build.")
                        .build()
                )
                .addModifiers(Modifier.PRIVATE)
                .addStatement("flyweight = new " + composite.getName() + "()")
                .addStatement("maxCapacity = capacity")
                .addStatement("repositoryBufferLength = capacity * "
                    +
                    composite.getName() + ".BUFFER_LENGTH")
                .addStatement("internalBuffer = new ExpandableDirectByteBuffer(repositoryBufferLength)")
                .addStatement("internalBuffer.setMemory(0, repositoryBufferLength, (byte)0)")
                .addStatement("offsetByKey = new Int2IntHashMap(Integer.MIN_VALUE)")
                .addStatement("unfilteredIterator = new UnfilteredIterator()")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("createWithCapacity")
                .addJavadoc("Creates a respository holding at most capacity elements.")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(int.class, CAPACITY)
                .returns(ClassName.get(composite.getPackageNameGen(), composite.getRepositoryName()))
                .addStatement("return new " + composite.getRepositoryName() + "(capacity)")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("appendWithKey")
                .addJavadoc("Appends an element in the buffer with the provided key. Key cannot be changed. ")
                .addJavadoc("Returns null if new element could not be created or if the key already exists.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .returns(ClassName.get(composite.getPackageNameGen(), composite.getName()))
                .beginControlFlow("if (currentCount >= maxCapacity)")
                .addStatement(RETURN_NULL)
                .endControlFlow()
                .beginControlFlow("if (offsetByKey.containsKey(id))")
                .addStatement(RETURN_NULL)
                .endControlFlow()
                .addStatement("flyweight.setUnderlyingBuffer(internalBuffer, maxUsedOffset)")
                .addStatement("offsetByKey.put(id, maxUsedOffset)")
                .addStatement("flyweight.write" + upperFirst(composite.getKeyName()) + "(id)")
                .addStatement("flyweight.lockKeyId()")
                .addStatement("currentCount += 1")
                .addStatement("maxUsedOffset = maxUsedOffset + " + composite.getName() + BUFFER_LENGTH_1)
                .addStatement(RETURN_FLYWEIGHT)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("containsKey")
                .addJavadoc("Returns true if the given key is known; false if not.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .returns(boolean.class)
                .addStatement("return offsetByKey.containsKey(id)")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getCurrentCount")
                .addJavadoc("Returns the number of elements currently in the repository.")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return currentCount")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getCapacity")
                .addJavadoc("Returns the maximum number of elements that can be stored in the repository.")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return maxCapacity")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getByKey")
                .addJavadoc("Moves the flyweight onto the buffer segment associated with the provided key. ")
                .addJavadoc("Returns null if not found.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .returns(ClassName.get(composite.getPackageNameGen(), composite.getName()))
                .beginControlFlow("if (offsetByKey.containsKey(id))")
                .addStatement("int offset = offsetByKey.get(id)")
                .addStatement("flyweight.setUnderlyingBuffer(internalBuffer, offset)")
                .addStatement("flyweight.lockKeyId()")
                .addStatement(RETURN_FLYWEIGHT)
                .endControlFlow()
                .addStatement(RETURN_NULL)
                .build()
        );

        final ClassName iterator = ClassName.get(JAVA_UTIL, ITERATOR);
        final ClassName genObj = ClassName.get("", composite.getName());
        final TypeName iteratorGen = ParameterizedTypeName.get(iterator, genObj);

        results.add(
            MethodSpec.methodBuilder("allItems")
                .addJavadoc("Returns iterator which returns all items. ")
                .addModifiers(Modifier.PUBLIC)
                .returns(iteratorGen)
                .addStatement("return unfilteredIterator")
                .build()
        );
        return results;
    }

    private void generateComposite(final ProcessingEnvironment pe,
                                   final PreprocessedEiderComposite composite,
                                   final AgronaWriterGlobalState globalState)
    {
        TypeSpec.Builder builder = TypeSpec.classBuilder(composite.getName())
            .addFields(buildCompositeFields(composite, globalState))
            .addMethods(buildCompositeMethods(composite))
            .addModifiers(Modifier.PUBLIC);
        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(composite.getPackageNameGen(), generated)
            .addFileComment("EIDER COMPOSITE GENERATED BY EIDER AT "
                + LocalDateTime.now(ZoneId.of("UTC")).toString()
                + "Z. ")
            .addFileComment("DO NOT MODIFY")
            .build();

        try
        { // write the file
            JavaFileObject source = pe.getFiler()
                .createSourceFile(composite.getPackageNameGen() + "." + composite.getName());
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file already existing after its first run, this is normal
        }
    }

    private Iterable<MethodSpec> buildCompositeMethods(PreprocessedEiderComposite composite)
    {
        List<MethodSpec> results = new ArrayList<>();

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addJavadoc("Constructor that allocates an internal buffer.")
            .addModifiers(Modifier.PUBLIC)
            .addStatement("initialOffset = 0")
            .addStatement("internalBuffer = new ExpandableDirectByteBuffer(BUFFER_LENGTH)")
            .addStatement("internalBuffer.setMemory(0, BUFFER_LENGTH, (byte)0)")
            .addStatement("internalBuffer.putShort(EIDER_ID_OFFSET, EIDER_ID)")
            .addStatement("internalBuffer.putInt(LENGTH_OFFSET, BUFFER_LENGTH)");

        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            constructor.addStatement(
                compositeItem.getName().toUpperCase()
                    +
                    "_FLYWEIGHT.setUnderlyingBuffer("
                    +
                    "internalBuffer, "
                    +
                    compositeItem.getName().toUpperCase()
                    +
                    "_OFFSET"
                    +
                    ")"
            );
            constructor.addStatement(
                compositeItem.getName().toUpperCase()
                    +
                    "_FLYWEIGHT.writeHeader()"
            );
        }

        results.add(constructor.build());

        MethodSpec.Builder constructorExistingBuffer = MethodSpec.constructorBuilder()
            .addJavadoc("Constructor that does not allocate, it sits atop an external buffer.")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ExpandableDirectByteBuffer.class, "bufferToUse")
            .addParameter(int.class, OFFSET)
            .addStatement("bufferToUse.checkLimit(offset + BUFFER_LENGTH)")
            .addStatement("initialOffset = offset")
            .addStatement("internalBuffer = bufferToUse")
            .addStatement("internalBuffer.putShort(offset + EIDER_ID_OFFSET, EIDER_ID)")
            .addStatement("internalBuffer.putInt(offset + LENGTH_OFFSET, BUFFER_LENGTH)");

        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            constructorExistingBuffer.addStatement(
                compositeItem.getName().toUpperCase()
                    +
                    "_FLYWEIGHT.setUnderlyingBuffer("
                    +
                    "internalBuffer, "
                    +
                    compositeItem.getName().toUpperCase()
                    +
                    "_OFFSET + initialOffset"
                    +
                    ")"
            );
            constructorExistingBuffer.addStatement(
                compositeItem.getName().toUpperCase()
                    +
                    "_FLYWEIGHT.writeHeader()"
            );
        }

        results.add(constructorExistingBuffer.build());

        MethodSpec.Builder setUnderlyingBuffer = MethodSpec.methodBuilder("setUnderlyingBuffer")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Updates the internal buffer and initial offset.")
            .addParameter(ExpandableDirectByteBuffer.class, BUFFER)
            .addParameter(int.class, OFFSET)
            .addStatement("internalBuffer = buffer")
            .addStatement("initialOffset = offset")
            .addStatement("keyLocked = false");

        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            setUnderlyingBuffer.addStatement(
                compositeItem.getName().toUpperCase()
                    +
                    "_FLYWEIGHT.setUnderlyingBuffer("
                    +
                    "internalBuffer, "
                    +
                    compositeItem.getName().toUpperCase()
                    +
                    "_OFFSET + initialOffset"
                    +
                    ")"
            );
        }

        MethodSpec setBuffer = setUnderlyingBuffer.build();

        results.add(setBuffer);

        if (composite.getKeyType().equals(EiderPropertyType.INT))
        {
            MethodSpec keyReader = MethodSpec.methodBuilder("read" + upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Reads the key value from the buffer.")
                .returns(int.class)
                .addStatement("return internalBuffer.getInt(initialOffset + KEY_FIELD_OFFSET)")
                .build();

            MethodSpec keyWriter = MethodSpec.methodBuilder(WRITE + upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Returns true if the provided key was written, false if not.")
                .addParameter(int.class, "key")
                .returns(boolean.class)
                .beginControlFlow("if (!keyLocked)")
                .addStatement("internalBuffer.putInt(initialOffset + KEY_FIELD_OFFSET, key)")
                .addStatement(RETURN_TRUE)
                .endControlFlow()
                .addStatement(RETURN_FALSE)
                .build();

            results.add(keyReader);
            results.add(keyWriter);
        }
        else
        {
            MethodSpec keyReader = MethodSpec.methodBuilder("read" + upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Reads the key value from the buffer.")
                .returns(long.class)
                .addStatement("return internalBuffer.getLong(initialOffset + KEY_FIELD_OFFSET)")
                .build();

            MethodSpec keyWriter = MethodSpec.methodBuilder(WRITE + upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Returns true if the provided key was written, false if not.")
                .addParameter(long.class, "key")
                .returns(boolean.class)
                .beginControlFlow("if (!keyLocked)")
                .addStatement("internalBuffer.putLong(initialOffset + KEY_FIELD_OFFSET, key)")
                .addStatement(RETURN_TRUE)
                .endControlFlow()
                .addStatement(RETURN_FALSE)
                .build();

            results.add(keyReader);
            results.add(keyWriter);
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder("lockKey" + upperFirst(composite.getKeyName()))
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Prevents any further updates to the key field.")
            .addStatement("keyLocked = true");
        results.add(builder.build());


        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            results.add(
                MethodSpec.methodBuilder("copy" + upperFirst(compositeItem.getName()) + "FromBuffer")
                    .addJavadoc("Copies " + compositeItem.getName() + " from the source to the buffer.")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ExpandableDirectByteBuffer.class, "sourceBuffer")
                    .addParameter(int.class, OFFSET)
                    .addStatement("internalBuffer.putBytes("
                        +
                        compositeItem.getName().toUpperCase()
                        +
                        "_OFFSET + initialOffset, sourceBuffer, offset, "
                        +
                        compositeItem.getObject().getName()
                        +
                        ".BUFFER_LENGTH)")
                    .build()
            );

            String flyWeight = compositeItem.getName().toUpperCase() + FLYWEIGHT;

            MethodSpec.Builder source = MethodSpec.methodBuilder("put" + upperFirst(compositeItem.getName()))
                .addJavadoc("Copies the contents from source to this " + compositeItem.getName() + " buffer.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("", upperFirst(compositeItem.getObject().getName())), "source");

            List<PreprocessedEiderProperty> propertyList = compositeItem.getObject().getPropertyList();

            for (final PreprocessedEiderProperty property : propertyList)
            {
                final String prop = upperFirst(property.getName());
                source.addStatement(flyWeight + ".write" + prop + "(source.read" + prop + "())");
            }

            results.add(source.build());

            results.add(
                MethodSpec.methodBuilder("get" + upperFirst(compositeItem.getName()))
                    .addJavadoc("Returns the " + upperFirst(compositeItem.getObject().getName()) + " flyweight.")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ClassName.get("", upperFirst(compositeItem.getObject().getName())))
                    .addStatement("return " + compositeItem.getName().toUpperCase() + FLYWEIGHT)
                    .build()
            );
        }

        return results;
    }

    private Iterable<FieldSpec> buildCompositeFields(PreprocessedEiderComposite composite,
                                                     AgronaWriterGlobalState globalState)
    {
        int bufferTotalLength = Integer.BYTES * 2;
        if (composite.getKeyType() == EiderPropertyType.INT)
        {
            bufferTotalLength += Integer.BYTES;
        }
        else
        {
            bufferTotalLength += Long.BYTES;
        }

        for (final PreprocessedNamedEiderObject obj : composite.getObjectList())
        {
            bufferTotalLength += globalState.getBufferLengths().get(obj.getObject().getName());
        }

        List<FieldSpec> fields = new ArrayList<>();

        fields.add(FieldSpec
            .builder(int.class, "BUFFER_LENGTH")
            .addJavadoc("The length of this composite object.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC)
            .initializer(Integer.toString(bufferTotalLength))
            .build());

        fields.add(FieldSpec
            .builder(int.class, "initialOffset")
            .addJavadoc("The initial offset in the buffer.")
            .addModifiers(Modifier.PRIVATE)
            .build());

        fields.add(FieldSpec
            .builder(short.class, "EIDER_ID")
            .addJavadoc("The eider ID of this composite object.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC)
            .initializer(Short.toString(composite.getEiderId()))
            .build());

        fields.add(FieldSpec
            .builder(short.class, "EIDER_GROUP_ID")
            .addJavadoc("The eider ID of this composite object.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC)
            .initializer(Short.toString((short) 1))
            .build());

        fields.add(FieldSpec
            .builder(boolean.class, "keyLocked")
            .addJavadoc("Indicates if the key is locked or not.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("false")
            .build());

        fields.add(FieldSpec
            .builder(int.class, "EIDER_ID_OFFSET")
            .addJavadoc("The offset of the EIDER ID.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.STATIC)
            .initializer(Integer.toString(0))
            .build());

        fields.add(FieldSpec
            .builder(int.class, "EIDER_GROUP_ID_OFFSET")
            .addJavadoc("The offset of the EIDER GROUP ID.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.STATIC)
            .initializer(Integer.toString(Short.BYTES))
            .build());

        fields.add(FieldSpec
            .builder(int.class, "LENGTH_OFFSET")
            .addJavadoc("The offset of the length of this composite object in the buffer.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.STATIC)
            .initializer(Integer.toString(Short.BYTES + Short.BYTES))
            .build());

        int currentPos = Integer.BYTES + Short.BYTES + Short.BYTES;

        if (composite.getKeyType() == EiderPropertyType.INT)
        {
            fields.add(FieldSpec
                .builder(int.class, "KEY_FIELD_OFFSET")
                .addJavadoc("The offset of the integer key of this composite object in the buffer.")
                .addModifiers(Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .initializer(Integer.toString(currentPos))
                .build());
            currentPos += Integer.BYTES;
        }
        else
        {
            fields.add(FieldSpec
                .builder(int.class, "KEY_FIELD_OFFSET")
                .addJavadoc("The offset of the long key of this composite object in the buffer.")
                .addModifiers(Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .initializer(Integer.toString(currentPos))
                .build());
            currentPos += Long.BYTES;
        }

        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            fields.add(FieldSpec
                .builder(int.class, compositeItem.getName().toUpperCase() + "_OFFSET")
                .addJavadoc("The offset of the " + compositeItem.getName() + " within the composite buffer.")
                .addModifiers(Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .initializer(Integer.toString(currentPos))
                .build());

            fields.add(FieldSpec
                .builder(ClassName.get("", compositeItem.getObject().getName()),
                    compositeItem.getName().toUpperCase() + FLYWEIGHT)
                .addJavadoc("The flyweight for the " + compositeItem.getName() + " within this buffer.")
                .addModifiers(Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .initializer("new " + compositeItem.getObject().getName() + "()")
                .build());

            currentPos = currentPos + globalState.getBufferLengths().get(compositeItem.getObject().getName());
        }

        fields.add(FieldSpec
            .builder(ExpandableDirectByteBuffer.class, INTERNAL_BUFFER)
            .addJavadoc("The internal buffer to hold this composite object.")
            .addModifiers(Modifier.PRIVATE)
            .build());

        return fields;
    }

    private void generateStandardFiles(ProcessingEnvironment pe, String packageName)
    {
        generateEiderHelper(pe, packageName);
    }

    private void generateEiderHelper(ProcessingEnvironment pe, final String packageName)
    {
        TypeSpec.Builder builder = TypeSpec.classBuilder("EiderHelper")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethods(buildHeaderHelperMethods());
        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(packageName, generated)
            .addFileComment("EIDER HELPER GENERATED BY EIDER AT "
                + LocalDateTime.now(ZoneId.of("UTC")).toString()
                + "Z. ")
            .addFileComment("DO NOT MODIFY")
            .build();

        try
        { // write the file
            JavaFileObject source = pe.getFiler()
                .createSourceFile(packageName + ".EiderHelper");
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file alreadyf existing after its first run, this is normal
        }
    }

    private Iterable<MethodSpec> buildHeaderHelperMethods()
    {
        List<MethodSpec> results = new ArrayList<>();

        results.add(
            MethodSpec.constructorBuilder()
                .addJavadoc("private constructor.")
                .addModifiers(Modifier.PRIVATE)
                .addStatement("//unused")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getEiderId")
                .addJavadoc("Reads the Eider Id from the buffer at the offset provided.")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(short.class)
                .addParameter(DirectBuffer.class, BUFFER)
                .addParameter(int.class, OFFSET)
                .addStatement("return buffer.getShort(offset" + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getEiderGroupId")
                .addJavadoc("Reads the Eider Group Id from the buffer at the offset provided.")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(short.class)
                .addParameter(DirectBuffer.class, BUFFER)
                .addParameter(int.class, OFFSET)
                .addStatement("return buffer.getShort(offset + 2"
                    +
                    JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1)
                .build()
        );

        return results;
    }

    private void generateSpecRepository(final ProcessingEnvironment pe, final PreprocessedEiderObject object)
    {
        String keyField = getKeyField(object);

        if (keyField == null)
        {
            throw new RuntimeException("Repository objects must have exactly one key field");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(object.getRepositoryName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethods(buildRepositoryMethods(object))
            .addFields(buildRepositoryFields(object))
            .addTypes(buildRepositoryIterators(object));

        if (object.isTransactionalRepository())
        {
            builder.addMethods(buildRepositoryTransactionalHelpers());
        }

        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(object.getPackageNameGen(), generated)
            .addFileComment("AGRONA REPOSITORY GENERATED BY EIDER AT "
                + LocalDateTime.now(ZoneId.of("UTC")).toString()
                + "Z. SPEC: ")
            .addFileComment(object.getClassNameInput())
            .addFileComment(". DO NOT MODIFY")
            .build();

        try
        { // write the file
            JavaFileObject source = pe.getFiler()
                .createSourceFile(object.getPackageNameGen() + "." + object.getRepositoryName());
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file already existing after its first run, this is normal
        }
    }

    private Iterable<MethodSpec> buildRepositoryTransactionalHelpers()
    {
        List<MethodSpec> results = new ArrayList<>();

        results.add(
            MethodSpec.methodBuilder("beginTransaction")
                .addJavadoc("Begins the transaction by making a temporary copy of the internal buffer. ")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("internalBuffer.getBytes(0, transactionCopy, 0, repositoryBufferLength)")
                .addStatement("offsetByKeyCopy.clear()")
                .addStatement("offsetByKey.forEach((k, v) -> offsetByKeyCopy.put(k, v))")
                .addStatement("currentCountCopy = currentCount")
                .addStatement("transactionCopyBufferSet = true")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("commit")
                .addJavadoc("Prevents rollback being called within a new call to beginTransaction.")
                .addModifiers(Modifier.PUBLIC)
                .addStatement(TRANSACTION_COPY_BUFFER_SET_FALSE)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("rollback")
                .addJavadoc("Restores the internal buffer to the state it was at when beginTransaction was called. ")
                .addJavadoc("Returns true if rollback was done; false if not. ")
                .addJavadoc("Will not rollback after a commit or before beginTransaction called.")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .beginControlFlow("if (transactionCopyBufferSet)")
                .addStatement("internalBuffer.putBytes(0, transactionCopy, 0, repositoryBufferLength)")
                .addStatement("offsetByKey.clear()")
                .addStatement("offsetByKeyCopy.forEach((k, v) -> offsetByKey.put(k, v))")
                .addStatement("offsetByKeyCopy.clear()")
                .addStatement("currentCount = currentCountCopy")
                .addStatement(TRANSACTION_COPY_BUFFER_SET_FALSE)
                .addStatement("currentCountCopy = 0")
                .addStatement(RETURN_TRUE)
                .endControlFlow()
                .addStatement(RETURN_FALSE)
                .build()
        );

        return results;
    }

    private Iterable<TypeSpec> buildRepositoryIterators(PreprocessedEiderObject object)
    {
        List<TypeSpec> results = new ArrayList<>();

        final ClassName iterator = ClassName.get(JAVA_UTIL, ITERATOR);
        final ClassName genObj = ClassName.get("", object.getName());
        final TypeName iteratorGen = ParameterizedTypeName.get(iterator, genObj);

        TypeSpec allItems = TypeSpec.classBuilder(UNFILTERED_ITERATOR)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .addSuperinterface(iteratorGen)
            .addField(FieldSpec.builder(ClassName.get(object.getPackageNameGen(), object.getName()),
                "iteratorFlyweight", Modifier.PRIVATE).initializer("new " + object.getName() + "()").build())
            .addField(FieldSpec.builder(int.class, "currentOffset", Modifier.PRIVATE).initializer("0").build())
            .addMethod(buildAllIteratorHasNext(object))
            .addMethod(buildAllIteratorNext(object))
            .addMethod(buildAllIteratorReset())
            .build();

        results.add(allItems);

        return results;
    }

    private MethodSpec buildAllIteratorReset()
    {
        return MethodSpec.methodBuilder("reset")
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get("", UNFILTERED_ITERATOR))
            .addStatement("currentOffset = 0")
            .addStatement("return this")
            .build();
    }

    private MethodSpec buildAllIteratorNext(PreprocessedEiderObject object)
    {
        return MethodSpec.methodBuilder("next")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(object.getPackageNameGen(), object.getName()))
            .beginControlFlow("if (hasNext())")
            .beginControlFlow("if (currentOffset > maxUsedOffset)")
            .addStatement(THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION)
            .endControlFlow()
            .addStatement("iteratorFlyweight.setUnderlyingBuffer(internalBuffer, currentOffset)")
            .addStatement("currentOffset = currentOffset + " + object.getName() + BUFFER_LENGTH_1)
            .addStatement("return iteratorFlyweight")
            .endControlFlow()
            .addStatement(THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION)
            .build();
    }

    private MethodSpec buildAllIteratorHasNext(PreprocessedEiderObject object)
    {
        return MethodSpec.methodBuilder("hasNext")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addStatement("return currentCount != 0 && (currentOffset + "
                +
                object.getName()
                +
                ".BUFFER_LENGTH + 1 <=maxUsedOffset)")
            .build();
    }

    private Iterable<MethodSpec> buildRepositoryMethods(PreprocessedEiderObject object)
    {
        List<MethodSpec> results = new ArrayList<>();


        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
            .addJavadoc("constructor")
            .addParameter(
                ParameterSpec.builder(int.class, CAPACITY)
                    .addJavadoc("capacity to build.")
                    .build()
            )
            .addModifiers(Modifier.PRIVATE)
            .addStatement("flyweight = new " + object.getName() + "()")
            .addStatement("maxCapacity = capacity")
            .addStatement("repositoryBufferLength = capacity * "
                +
                object.getName() + ".BUFFER_LENGTH")
            .addStatement("internalBuffer = new ExpandableDirectByteBuffer(repositoryBufferLength)")
            .addStatement("internalBuffer.setMemory(0, repositoryBufferLength, (byte)0)")
            .addStatement("offsetByKey = new Int2IntHashMap(Integer.MIN_VALUE)")
            .addStatement("unfilteredIterator = new UnfilteredIterator()");

        if (object.isTransactionalRepository())
        {
            builder.addStatement("transactionCopy = new ExpandableDirectByteBuffer(repositoryBufferLength)");
        }

        results.add(builder.build());

        results.add(
            MethodSpec.methodBuilder("createWithCapacity")
                .addJavadoc("Creates a respository holding at most capacity elements.")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(int.class, CAPACITY)
                .returns(ClassName.get(object.getPackageNameGen(), object.getRepositoryName()))
                .addStatement("return new " + object.getRepositoryName() + "(capacity)")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("appendWithKey")
                .addJavadoc("Appends an element in the buffer with the provided key. Key cannot be changed. ")
                .addJavadoc("Returns null if new element could not be created or if the key already exists.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .returns(ClassName.get(object.getPackageNameGen(), object.getName()))
                .beginControlFlow("if (currentCount >= maxCapacity)")
                .addStatement(RETURN_NULL)
                .endControlFlow()
                .beginControlFlow("if (offsetByKey.containsKey(id))")
                .addStatement(RETURN_NULL)
                .endControlFlow()
                .addStatement("flyweight.setUnderlyingBuffer(internalBuffer, maxUsedOffset)")
                .addStatement("offsetByKey.put(id, maxUsedOffset)")
                .addStatement("flyweight.writeHeader()")
                .addStatement("flyweight.write" + upperFirst(getKeyField(object)) + "(id)")
                .addStatement("flyweight.lockKeyId()")
                .addStatement("currentCount += 1")
                .addStatement("maxUsedOffset = maxUsedOffset + " + object.getName() + BUFFER_LENGTH_1)
                .addStatement(RETURN_FLYWEIGHT)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("containsKey")
                .addJavadoc("Returns true if the given key is known; false if not.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .returns(boolean.class)
                .addStatement("return offsetByKey.containsKey(id)")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getCurrentCount")
                .addJavadoc("Returns the number of elements currently in the repository.")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return currentCount")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getCapacity")
                .addJavadoc("Returns the maximum number of elements that can be stored in the repository.")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return maxCapacity")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("dumpBuffer")
                .addJavadoc("Returns the internal buffer as a byte[]. Warning! Allocates.")
                .addModifiers(Modifier.PRIVATE)
                .returns(ArrayTypeName.of(byte.class))
                .addStatement("byte[] tmpBuffer = new byte[repositoryBufferLength]")
                .addStatement("internalBuffer.getBytes(0, tmpBuffer)")
                .addStatement("return tmpBuffer")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getByKey")
                .addJavadoc("Moves the flyweight onto the buffer segment associated with the provided key. ")
                .addJavadoc("Returns null if not found.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .returns(ClassName.get(object.getPackageNameGen(), object.getName()))
                .beginControlFlow("if (offsetByKey.containsKey(id))")
                .addStatement("int offset = offsetByKey.get(id)")
                .addStatement("flyweight.setUnderlyingBuffer(internalBuffer, offset)")
                .addStatement("flyweight.lockKeyId()")
                .addStatement(RETURN_FLYWEIGHT)
                .endControlFlow()
                .addStatement(RETURN_NULL)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("getCrc32")
                .addJavadoc("Returns the CRC32 of the underlying buffer. Warning! Allocates.")
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addStatement("crc32.reset()")
                .addStatement("crc32.update(dumpBuffer())")
                .addStatement("return crc32.getValue()")
                .build()
        );

        final ClassName iterator = ClassName.get(JAVA_UTIL, ITERATOR);
        final ClassName genObj = ClassName.get("", object.getName());
        final TypeName iteratorGen = ParameterizedTypeName.get(iterator, genObj);

        results.add(
            MethodSpec.methodBuilder("allItems")
                .addJavadoc("Returns iterator which returns all items. ")
                .addModifiers(Modifier.PUBLIC)
                .returns(iteratorGen)
                .addStatement("return unfilteredIterator")
                .build()
        );

        return results;
    }

    private Iterable<FieldSpec> buildRepositoryFields(PreprocessedEiderObject object)
    {
        List<FieldSpec> results = new ArrayList<>();

        results.add(FieldSpec
            .builder(ExpandableDirectByteBuffer.class, INTERNAL_BUFFER)
            .addJavadoc("The internal MutableDirectBuffer holding capacity instances.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(Int2IntHashMap.class, "offsetByKey")
            .addJavadoc("For mapping the key to the offset.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(CRC32.class, "crc32")
            .addJavadoc("Used to compute CRC32 of the underlying buffer")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .initializer("new CRC32()")
            .build());

        results.add(FieldSpec
            .builder(int.class, "maxUsedOffset")
            .addJavadoc("The current max offset used of the buffer.")
            .initializer("0")
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(int.class, "currentCount")
            .addJavadoc("The current count of elements in the buffer.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("0")
            .build());

        results.add(FieldSpec
            .builder(int.class, "maxCapacity")
            .addJavadoc("The maximum count of elements in the buffer.")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build());

        results.add(FieldSpec
            .builder(ClassName.get("", UNFILTERED_ITERATOR), "unfilteredIterator")
            .addJavadoc("The iterator for unfiltered items.")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build());

        results.add(FieldSpec
            .builder(int.class, "repositoryBufferLength")
            .addJavadoc("The length of the internal buffer.")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build());

        results.add(FieldSpec.builder(ClassName.get(object.getPackageNameGen(), object.getName()), "flyweight")
            .addJavadoc("The flyweight used by the repository.")
            .initializer("null")
            .addModifiers(Modifier.PRIVATE)
            .build());

        if (object.isTransactionalRepository())
        {
            results.add(FieldSpec
                .builder(Int2IntHashMap.class, "offsetByKeyCopy")
                .addJavadoc("The offsets by key at time of beginTransaction.")
                .addModifiers(Modifier.PRIVATE)
                .initializer("new Int2IntHashMap(Integer.MIN_VALUE)")
                .build());

            results.add(FieldSpec
                .builder(boolean.class, "transactionCopyBufferSet")
                .addJavadoc("Flag which defines if the transaction copy buffer is set.")
                .addModifiers(Modifier.PRIVATE)
                .initializer("false")
                .build());

            results.add(FieldSpec
                .builder(int.class, "currentCountCopy")
                .addJavadoc("The current count of elements in the buffer.")
                .addModifiers(Modifier.PRIVATE)
                .initializer("0")
                .build());

            results.add(FieldSpec
                .builder(ExpandableDirectByteBuffer.class, "transactionCopy")
                .addJavadoc("The MutableDirectBuffer used internally for rollbacks.")
                .addModifiers(Modifier.PRIVATE)
                .build());

            results.add(FieldSpec
                .builder(int.class, "transactionCopyLength")
                .addJavadoc("The current length of the transactionCopy buffer.")
                .addModifiers(Modifier.PRIVATE)
                .initializer("0")
                .build());
        }

        return results;
    }

    private String getKeyField(PreprocessedEiderObject object)
    {
        for (final PreprocessedEiderProperty property : object.getPropertyList())
        {
            if (property.getAnnotations().get(Constants.KEY).equalsIgnoreCase(TRUE))
            {
                return property.getName();
            }
        }
        return null;
    }

    private void generateSpecObject(final ProcessingEnvironment processingEnv, final PreprocessedEiderObject object,
                                    final AgronaWriterState state, final AgronaWriterGlobalState globalState)
    {
        TypeSpec.Builder builder = TypeSpec.classBuilder(object.getName())
            .addModifiers(Modifier.PUBLIC)
            .addField(buildEiderIdField(object.getEiderId()))
            .addField(buildEiderGroupIdField(object.getEiderGroupId()))
            .addFields(offsetsForFields(object, state, globalState))
            .addFields(internalFields(object))
            .addMethod(buildBuffer(object))
            .addMethod(buildEiderId())
            .addMethods(forInternalFields(object));

        if (object.isTransactional())
        {
            builder.addMethods(buildTransactionHelpers());
        }
        else
        {
            builder.addMethods(buildNonTransactionHelpers());
        }

        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(object.getPackageNameGen(), generated)
            .addFileComment("AGRONA FLYWEIGHT GENERATED BY EIDER AT "
                + LocalDateTime.now(ZoneId.of("UTC")).toString()
                + "Z. SPEC: ")
            .addFileComment(object.getClassNameInput())
            .addFileComment(". DO NOT MODIFY")
            .build();

        try
        { // write the file
            JavaFileObject source = processingEnv.getFiler()
                .createSourceFile(object.getPackageNameGen() + "." + object.getName());
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file already existing after its first run, this is normal
        }
    }

    private Iterable<MethodSpec> buildNonTransactionHelpers()
    {
        List<MethodSpec> results = new ArrayList<>();

        results.add(
            MethodSpec.methodBuilder("supportsTransactions")
                .addJavadoc("True if transactions are supported; false if not.")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement(RETURN_FALSE)
                .build()
        );

        return results;
    }

    private Iterable<MethodSpec> buildTransactionHelpers()
    {
        List<MethodSpec> results = new ArrayList<>();

        results.add(
            MethodSpec.methodBuilder("supportsTransactions")
                .addJavadoc("True if transactions are supported; false if not.")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement(RETURN_TRUE)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("beginTransaction")
                .addJavadoc("Begins the transaction by making a temporary copy of the internal buffer.")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("buffer.getBytes(0, transactionCopy, 0, BUFFER_LENGTH)")
                .addStatement("transactionCopyBufferSet = true")
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("commit")
                .addJavadoc("Prevents rollback being called within a new call to beginTransaction.")
                .addModifiers(Modifier.PUBLIC)
                .addStatement(TRANSACTION_COPY_BUFFER_SET_FALSE)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("rollback")
                .addJavadoc("Restores the internal buffer to the state it was at when beginTransaction was called. ")
                .addJavadoc("Returns true if rollback was done; false if not. ")
                .addJavadoc("Will not rollback after a commit or before beginTransaction called.")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .beginControlFlow("if (transactionCopyBufferSet)")
                .addStatement("if (!isMutable) throw new RuntimeException(\"cannot write to immutable buffer\")")
                .addStatement("mutableBuffer.putBytes(0, transactionCopy, 0, BUFFER_LENGTH)")
                .addStatement(TRANSACTION_COPY_BUFFER_SET_FALSE)
                .addStatement(RETURN_TRUE)
                .endControlFlow()
                .addStatement(RETURN_FALSE)
                .build()
        );

        return results;
    }

    private Iterable<FieldSpec> internalFields(PreprocessedEiderObject object)
    {
        List<FieldSpec> results = new ArrayList<>();

        results.add(FieldSpec
            .builder(DirectBuffer.class, BUFFER)
            .addJavadoc("The internal DirectBuffer.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("null")
            .build());

        results.add(FieldSpec
            .builder(MutableDirectBuffer.class, MUTABLE_BUFFER)
            .addJavadoc("The internal DirectBuffer used for mutatation opertions. "
                +
                "Valid only if a mutable buffer was provided.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("null")
            .build());

        results.add(FieldSpec
            .builder(UnsafeBuffer.class, UNSAFE_BUFFER)
            .addJavadoc("The internal UnsafeBuffer. Valid only if an unsafe buffer was provided.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("null")
            .build());

        results.add(FieldSpec
            .builder(int.class, "initialOffset")
            .addJavadoc("The starting offset for reading and writing.")
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(boolean.class, "isMutable")
            .addJavadoc("Flag indicating if the buffer is mutable.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("false")
            .build());

        results.add(FieldSpec
            .builder(boolean.class, "isUnsafe")
            .addJavadoc("Flag indicating if the buffer is an UnsafeBuffer.")
            .addModifiers(Modifier.PRIVATE)
            .initializer("false")
            .build());

        results.add(FieldSpec
            .builder(boolean.class, "FIXED_LENGTH")
            .addJavadoc("Indicates if this flyweight holds a fixed length object.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.FINAL)
            .initializer(Boolean.toString(true))
            .build());

        if (object.isTransactional())
        {
            results.add(FieldSpec
                .builder(boolean.class, "transactionCopyBufferSet")
                .addJavadoc("Flag which defines if the transaction copy buffer is set.")
                .addModifiers(Modifier.PRIVATE)
                .initializer("false")
                .build());

            results.add(FieldSpec
                .builder(ExpandableDirectByteBuffer.class, "transactionCopy")
                .addJavadoc("The MutableDirectBuffer used internally for rollbacks.")
                .initializer("new ExpandableDirectByteBuffer(BUFFER_LENGTH)")
                .addModifiers(Modifier.PRIVATE)
                .build());
        }

        if (containsKeyField(object))
        {
            results.add(FieldSpec
                .builder(boolean.class, "keyLocked")
                .addJavadoc("Internal field to support the lockKey method.")
                .initializer("false")
                .addModifiers(Modifier.PRIVATE)
                .build());
        }

        return results;
    }

    private boolean containsKeyField(PreprocessedEiderObject object)
    {
        for (final PreprocessedEiderProperty property : object.getPropertyList())
        {
            if (property.getAnnotations() != null && property.getAnnotations().containsKey(Constants.KEY))
            {
                if (property.getAnnotations().get(Constants.KEY).equalsIgnoreCase(TRUE))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private Iterable<FieldSpec> offsetsForFields(PreprocessedEiderObject object,
                                                 AgronaWriterState state,
                                                 AgronaWriterGlobalState globalState)
    {
        List<FieldSpec> results = new ArrayList<>();

        results.add(FieldSpec
            .builder(int.class, "HEADER_OFFSET")
            .addJavadoc("The offset for the EIDER_ID within the buffer.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .initializer(Integer.toString(state.getCurrentOffset()))
            .build());

        state.extendCurrentOffset(Short.BYTES);

        results.add(FieldSpec
            .builder(int.class, "HEADER_GROUP_OFFSET")
            .addJavadoc("The offset for the EIDER_GROUP_IP within the buffer.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .initializer(Integer.toString(state.getCurrentOffset()))
            .build());

        state.extendCurrentOffset(Short.BYTES);

        results.add(FieldSpec
            .builder(int.class, "LENGTH_OFFSET")
            .addJavadoc("The length offset. Required for segmented buffers.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .initializer(Integer.toString(state.getCurrentOffset()))
            .build());

        state.extendCurrentOffset(Integer.BYTES);

        for (final PreprocessedEiderProperty property : object.getPropertyList())
        {
            results.add(genOffset(property, state));
        }

        results.add(FieldSpec
            .builder(int.class, "BUFFER_LENGTH")
            .addJavadoc("The total bytes required to store the object.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.FINAL)
            .initializer(Integer.toString(state.getCurrentOffset()))
            .build());

        globalState.getBufferLengths().put(object.getName(), state.getCurrentOffset());

        return results;
    }

    private FieldSpec genOffset(PreprocessedEiderProperty property,
                                AgronaWriterState runningOffset)
    {
        int bytes = byteLength(property.getType(), property.getAnnotations());
        int startAt = runningOffset.getCurrentOffset();
        runningOffset.extendCurrentOffset(bytes);

        return FieldSpec
            .builder(int.class, getOffsetName(property.getName()))
            .addJavadoc("The byte offset in the byte array for this " + property.getType().name()
                + ". Byte length is " + bytes + ".")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .initializer(Integer.toString(startAt))
            .build();
    }

    private String getOffsetName(String name)
    {
        return name.toUpperCase() + "_OFFSET";
    }

    private Iterable<MethodSpec> forInternalFields(PreprocessedEiderObject object)
    {
        List<PreprocessedEiderProperty> propertyList = object.getPropertyList();
        List<MethodSpec> results = new ArrayList<>();

        results.add(
            MethodSpec.methodBuilder("writeHeader")
                .addJavadoc("Writes the header data to the buffer.")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("if (!isMutable) throw new RuntimeException(\"cannot write to immutable buffer\")")
                .addStatement("mutableBuffer.putShort(initialOffset + HEADER_OFFSET"
                    +
                    ", EIDER_ID, "
                    + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN)
                .addStatement("mutableBuffer.putShort(initialOffset + HEADER_GROUP_OFFSET"
                    +
                    ", EIDER_GROUP_ID, "
                    + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN)
                .addStatement("mutableBuffer.putInt(initialOffset + LENGTH_OFFSET"
                    +
                    ", BUFFER_LENGTH, "
                    + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN)
                .build()
        );

        results.add(
            MethodSpec.methodBuilder("validateHeader")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Validates the length and eiderSpecId in the header "
                    + "against the expected values. False if invalid.")
                .returns(boolean.class)
                .addStatement("final short eiderId = buffer.getShort(initialOffset + HEADER_OFFSET"
                    + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1)
                .addStatement("final short eiderGroupId = buffer.getShort(initialOffset + HEADER_GROUP_OFFSET"
                    + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1)
                .addStatement("final int bufferLength = buffer.getInt(initialOffset + LENGTH_OFFSET"
                    + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1)
                .addStatement("if (eiderId != EIDER_ID) return false")
                .addStatement("if (eiderGroupId != EIDER_GROUP_ID) return false")
                .addStatement("return bufferLength == BUFFER_LENGTH")
                .build()
        );

        for (final PreprocessedEiderProperty property : propertyList)
        {
            results.add(genReadProperty(property));
            if (!property.getAnnotations().get(Constants.SEQUENCE_GENERATOR).equalsIgnoreCase(TRUE))
            {
                results.add(genWriteProperty(property));
            }
            if (property.getAnnotations() != null)
            {
                if (property.getAnnotations().get(Constants.KEY).equalsIgnoreCase(TRUE))
                {
                    results.add(getKeyLock(property));
                }
                if (property.getAnnotations().get(Constants.SEQUENCE_GENERATOR).equalsIgnoreCase(TRUE))
                {
                    results.add(buildSequenceGenerator(property));
                    results.add(buildSequenceInitialize(property));
                }
            }
        }

        return results;
    }

    private MethodSpec buildSequenceInitialize(PreprocessedEiderProperty property)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize" + upperFirst(property.getName()))
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Initializes " + property.getName() + " to the provided value. ")
            .addParameter(getInputType(property));

        if (property.getAnnotations() != null)
        {
            if (property.getAnnotations().get(Constants.KEY).equalsIgnoreCase(TRUE))
            {
                builder.addStatement("if (keyLocked) throw new RuntimeException(\"Cannot write key after locking\")");
                builder.addJavadoc("This field is marked key=true.");
            }
        }

        builder.addStatement(bufferWrite(property));
        return builder.build();
    }

    private MethodSpec buildSequenceGenerator(PreprocessedEiderProperty property)
    {
        final String read = "read" + upperFirst(property.getName());
        final String init = "initialize" + upperFirst(property.getName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder("next" + upperFirst(property.getName())
            +
            "Sequence")
            .addModifiers(Modifier.PUBLIC)
            .returns(fromType(property.getType()))
            .addJavadoc("Increments and returns the sequence in field " + property.getName() + ".")
            .addStatement("final " + fromTypeToStr(property.getType()) + " currentVal = " + read + "()")
            .addStatement(init + "(currentVal + 1)")
            .addStatement("return " + read + "()");

        return builder.build();
    }

    private MethodSpec getKeyLock(PreprocessedEiderProperty property)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("lockKey" + upperFirst(property.getName()))
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Prevents any further updates to the key field.")
            .addStatement("keyLocked = true");

        return builder.build();
    }

    private MethodSpec genWriteProperty(PreprocessedEiderProperty property)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(WRITE + upperFirst(property.getName()))
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Writes " + property.getName() + " to the buffer. ")
            .addParameter(getInputType(property));

        builder.addStatement("if (!isMutable) throw new RuntimeException(\"Cannot write to immutable buffer\")");


        if (property.getAnnotations() != null)
        {
            if (property.getAnnotations().get(Constants.KEY).equalsIgnoreCase(TRUE))
            {
                builder.addStatement("if (keyLocked) throw new RuntimeException(\"Cannot write key after locking\")");
                builder.addJavadoc("This field is marked key=true.");
            }
        }

        if (property.getType() == EiderPropertyType.FIXED_STRING)
        {
            builder.addStatement(fixedLengthStringCheck(property));
        }

        builder.addStatement(bufferWrite(property));

        return builder.build();
    }

    private ParameterSpec getInputType(PreprocessedEiderProperty property)
    {
        return ParameterSpec.builder(fromType(property.getType()), "value")
            .addJavadoc("Value for the " + property.getName() + " to write to buffer.")
            .build();
    }

    private String fixedLengthStringCheck(PreprocessedEiderProperty property)
    {
        int maxLength = Integer.parseInt(property.getAnnotations().get(Constants.MAXLENGTH));

        return "if (value.length() > " + maxLength + ") throw new RuntimeException(\"Field "
            + property.getName() + " is longer than maxLength=" + maxLength + "\")";
    }

    private String bufferWrite(PreprocessedEiderProperty property)
    {
        if (property.getType() == EiderPropertyType.INT)
        {
            return "mutableBuffer.putInt(initialOffset + " + getOffsetName(property.getName())
                +
                ", value, "
                + JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN;
        }
        else if (property.getType() == EiderPropertyType.LONG)
        {
            return "mutableBuffer.putLong(initialOffset + " + getOffsetName(property.getName())
                +
                ", value, java.nio.ByteOrder.LITTLE_ENDIAN)";
        }
        else if (property.getType() == EiderPropertyType.FIXED_STRING)
        {
            return "mutableBuffer.putStringWithoutLengthAscii(initialOffset + " + getOffsetName(property.getName())
                +
                ", value)";
        }
        else if (property.getType() == EiderPropertyType.BOOLEAN)
        {
            return "mutableBuffer.putByte(initialOffset + " + getOffsetName(property.getName())
                +
                ", value ? (byte)1 : (byte)0)";
        }
        return "// unsupported type " + property.getType().name();
    }

    private String bufferLimitCheck()
    {
        return "buffer.checkLimit(initialOffset + BUFFER_LENGTH)";
    }

    private MethodSpec genReadProperty(PreprocessedEiderProperty property)
    {
        return MethodSpec.methodBuilder("read" + upperFirst(property.getName()))
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Reads " + property.getName() + " as stored in the buffer.")
            .returns(fromType(property.getType()))
            .addStatement(bufferRead(property))
            .build();
    }

    private String bufferRead(PreprocessedEiderProperty property)
    {
        if (property.getType() == EiderPropertyType.INT)
        {
            return "return buffer.getInt(initialOffset + " + getOffsetName(property.getName())
                +
                JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1;
        }
        else if (property.getType() == EiderPropertyType.LONG)
        {
            return "return buffer.getLong(initialOffset + " + getOffsetName(property.getName())
                +
                JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1;
        }
        else if (property.getType() == EiderPropertyType.FIXED_STRING)
        {
            int length = Integer.parseInt(property.getAnnotations().get(Constants.MAXLENGTH));
            return "return buffer.getStringWithoutLengthAscii(initialOffset + " + getOffsetName(property.getName())
                +
                ", " + length + ").trim()";
        }
        else if (property.getType() == EiderPropertyType.BOOLEAN)
        {
            return "return buffer.getByte(initialOffset + " + getOffsetName(property.getName())
                +
                ") == (byte)1";
        }
        return "// unsupported type " + property.getType().name();
    }


    private FieldSpec buildEiderIdField(short eiderId)
    {
        return FieldSpec
            .builder(short.class, "EIDER_ID")
            .addJavadoc("The eider spec id for this type. Useful in switch statements to detect type in first 16bits.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.FINAL)
            .initializer(Short.toString(eiderId))
            .build();
    }

    private FieldSpec buildEiderGroupIdField(short groupId)
    {
        return FieldSpec
            .builder(short.class, "EIDER_GROUP_ID")
            .addJavadoc("The eider group id for this type. "
                +
                "Useful in switch statements to detect group in second 16bits.")
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.FINAL)
            .initializer(Short.toString(groupId))
            .build();
    }

    private MethodSpec buildEiderId()
    {
        return MethodSpec.methodBuilder("eiderId")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Returns the eider sequence.\n"
                +
                "@return EIDER_ID.\n")
            .returns(short.class)
            .addStatement("return EIDER_ID")
            .build();

    }

    private MethodSpec buildBuffer(PreprocessedEiderObject object)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("setUnderlyingBuffer")
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addJavadoc("Uses the provided {@link org.agrona.DirectBuffer} from the given offset.\n"
                +
                "@param buffer - buffer to read from and write to.\n"
                +
                "@param offset - offset to begin reading from/writing to in the buffer.\n")
            .addParameter(DirectBuffer.class, BUFFER)
            .addParameter(int.class, OFFSET)
            .addStatement("this.initialOffset = offset")
            .addStatement("this.buffer = buffer")
            .beginControlFlow("if (buffer instanceof UnsafeBuffer)")
            .addStatement(UNSAFE_BUFFER + " = (UnsafeBuffer) buffer")
            .addStatement(MUTABLE_BUFFER + " = (MutableDirectBuffer) buffer")
            .addStatement("isUnsafe = true")
            .addStatement("isMutable = true")
            .endControlFlow()
            .beginControlFlow("else if (buffer instanceof MutableDirectBuffer)")
            .addStatement(MUTABLE_BUFFER + " = (MutableDirectBuffer) buffer")
            .addStatement("isUnsafe = false")
            .addStatement("isMutable = true")
            .endControlFlow()
            .beginControlFlow("else")
            .addStatement("isUnsafe = false")
            .addStatement("isMutable = false")
            .endControlFlow();

        if (object.isTransactional())
        {
            builder.addStatement(TRANSACTION_COPY_BUFFER_SET_FALSE);
        }

        if (containsKeyField(object))
        {
            builder.addStatement("keyLocked = false");
        }

        builder.addStatement(bufferLimitCheck());
        return builder.build();
    }

    private String upperFirst(String input)
    {
        if (input == null)
        {
            throw new RuntimeException("Illegal input for upperFirst");
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private Class fromType(EiderPropertyType type)
    {
        switch (type)
        {
            case INT:
                return int.class;
            case SHORT:
                return short.class;
            case LONG:
                return long.class;
            case BOOLEAN:
                return boolean.class;
            case CHAR8:
                return char.class;
            case VAR_STRING:
            case FIXED_STRING:
                return String.class;
            default:
                return int.class;
        }
    }

    private int byteLength(EiderPropertyType type, Map<String, String> annotations)
    {
        switch (type)
        {
            case INT:
                return Integer.BYTES;
            case LONG:
                return Long.BYTES;
            case BOOLEAN:
                return 1;
            case SHORT:
                return Short.BYTES;
            case CHAR8:
                return Character.BYTES;
            case FIXED_STRING:
                return Integer.parseInt(annotations.get(Constants.MAXLENGTH));
            case VAR_STRING:
                throw new RuntimeException("Agrona writer does not support variable length strings");
            default:
                return Integer.BYTES;
        }
    }

    private String fromTypeToStr(EiderPropertyType type)
    {
        switch (type)
        {
            case INT:
                return "int";
            case LONG:
                return "long";
            case SHORT:
                return "short";
            case BOOLEAN:
                return "boolean";
            case CHAR8:
                return "char";
            case FIXED_STRING:
            case VAR_STRING:
                return "String";
            default:
                return "invalid";
        }
    }
}

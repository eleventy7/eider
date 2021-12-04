package io.eider.javawriter.agrona;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.eider.internals.EiderPropertyType;
import io.eider.internals.PreprocessedEiderComposite;
import io.eider.internals.PreprocessedEiderProperty;
import io.eider.internals.PreprocessedNamedEiderObject;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.collections.Int2IntHashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class AgronaCompositeGenerator
{

    public void generateCompositeRepository(final ProcessingEnvironment pe,
                                            final PreprocessedEiderComposite composite)
    {
        String keyField = composite.getKeyName();

        if (keyField == null)
        {
            throw new AgronaWriterException("Repository objects must have exactly one key field");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(composite.getRepositoryName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethods(buildCompositeRepositoryMethods(composite))
            .addFields(buildCompositeRepositoryFields(composite))
            .addTypes(buildCompositeRepositoryIterators(composite));

        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(composite.getPackageNameGen(), generated)
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

        final ClassName iterator = ClassName.get(Constants.JAVA_UTIL, Constants.ITERATOR);
        final ClassName genObj = ClassName.get("", composite.getName());
        final TypeName iteratorGen = ParameterizedTypeName.get(iterator, genObj);

        TypeSpec allItems = TypeSpec.classBuilder(Constants.UNFILTERED_ITERATOR)
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
            .returns(ClassName.get("", Constants.UNFILTERED_ITERATOR))
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
            .addStatement(Constants.THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION)
            .endControlFlow()
            .addStatement("iteratorFlyweight.setUnderlyingBuffer(internalBuffer, currentOffset)")
            .addStatement("currentOffset = currentOffset + " + composite.getName() + Constants.BUFFER_LENGTH_1)
            .addStatement("return iteratorFlyweight")
            .endControlFlow()
            .addStatement(Constants.THROW_NEW_JAVA_UTIL_NO_SUCH_ELEMENT_EXCEPTION)
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
            .builder(Int2IntHashMap.class, "offsetByKey")
            .addJavadoc("For mapping the key to the offset.")
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PRIVATE)
            .build());

        results.add(FieldSpec
            .builder(ExpandableDirectByteBuffer.class, Constants.INTERNAL_BUFFER)
            .addJavadoc("The internal MutableDirectBuffer holding capacity instances.")
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
            .builder(ClassName.get("", Constants.UNFILTERED_ITERATOR), "unfilteredIterator")
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
                    ParameterSpec.builder(int.class, Constants.CAPACITY)
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
                .addParameter(int.class, Constants.CAPACITY)
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
                .addStatement(Constants.RETURN_NULL)
                .endControlFlow()
                .beginControlFlow("if (offsetByKey.containsKey(id))")
                .addStatement(Constants.RETURN_NULL)
                .endControlFlow()
                .addStatement("flyweight.setUnderlyingBuffer(internalBuffer, maxUsedOffset)")
                .addStatement("offsetByKey.put(id, maxUsedOffset)")
                .addStatement("flyweight.write" + Util.upperFirst(composite.getKeyName()) + "(id)")
                .addStatement("flyweight.lockKeyId()")
                .addStatement("currentCount += 1")
                .addStatement("maxUsedOffset = maxUsedOffset + " + composite.getName() + Constants.BUFFER_LENGTH_1)
                .addStatement(Constants.RETURN_FLYWEIGHT)
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
                .addStatement(Constants.RETURN_FLYWEIGHT)
                .endControlFlow()
                .addStatement(Constants.RETURN_NULL)
                .build()
        );

        final ClassName iterator = ClassName.get(Constants.JAVA_UTIL, Constants.ITERATOR);
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

    public void generateComposite(final ProcessingEnvironment pe,
                                   final PreprocessedEiderComposite composite,
                                   final AgronaWriterGlobalState globalState)
    {
        TypeSpec.Builder builder = TypeSpec.classBuilder(composite.getName())
            .addFields(buildCompositeFields(composite, globalState))
            .addMethods(buildCompositeMethods(composite))
            .addModifiers(Modifier.PUBLIC);
        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(composite.getPackageNameGen(), generated)
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
                    Constants.FLYWEIGHT_SET_UNDERLYING_BUFFER_INTERNAL_BUFFER
                    +
                    compositeItem.getName().toUpperCase()
                    +
                    "_OFFSET)"
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
            .addParameter(int.class, Constants.OFFSET)
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
                    Constants.FLYWEIGHT_SET_UNDERLYING_BUFFER_INTERNAL_BUFFER
                    +
                    compositeItem.getName().toUpperCase()
                    +
                    "_OFFSET + initialOffset)"
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
            .addParameter(ExpandableDirectByteBuffer.class, Constants.BUFFER)
            .addParameter(int.class, Constants.OFFSET)
            .addStatement("internalBuffer = buffer")
            .addStatement("initialOffset = offset")
            .addStatement("keyLocked = false");

        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            setUnderlyingBuffer.addStatement(
                compositeItem.getName().toUpperCase()
                    +
                    Constants.FLYWEIGHT_SET_UNDERLYING_BUFFER_INTERNAL_BUFFER
                    +
                    compositeItem.getName().toUpperCase()
                    +
                    "_OFFSET + initialOffset)"
            );
        }

        MethodSpec setBuffer = setUnderlyingBuffer.build();

        results.add(setBuffer);

        if (composite.getKeyType().equals(EiderPropertyType.INT))
        {
            MethodSpec keyReader = MethodSpec.methodBuilder("read" + Util.upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Reads the key value from the buffer.")
                .returns(int.class)
                .addStatement("return internalBuffer.getInt(initialOffset + KEY_FIELD_OFFSET)")
                .build();

            MethodSpec keyWriter = MethodSpec.methodBuilder(Constants.WRITE + Util.upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Returns true if the provided key was written, false if not.")
                .addParameter(int.class, "key")
                .returns(boolean.class)
                .beginControlFlow("if (!keyLocked)")
                .addStatement("internalBuffer.putInt(initialOffset + KEY_FIELD_OFFSET, key)")
                .addStatement(Constants.RETURN_TRUE)
                .endControlFlow()
                .addStatement(Constants.RETURN_FALSE)
                .build();

            results.add(keyReader);
            results.add(keyWriter);
        }
        else
        {
            MethodSpec keyReader = MethodSpec.methodBuilder("read" + Util.upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Reads the key value from the buffer.")
                .returns(long.class)
                .addStatement("return internalBuffer.getLong(initialOffset + KEY_FIELD_OFFSET)")
                .build();

            MethodSpec keyWriter = MethodSpec.methodBuilder(Constants.WRITE + Util.upperFirst(composite.getKeyName()))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Returns true if the provided key was written, false if not.")
                .addParameter(long.class, "key")
                .returns(boolean.class)
                .beginControlFlow("if (!keyLocked)")
                .addStatement("internalBuffer.putLong(initialOffset + KEY_FIELD_OFFSET, key)")
                .addStatement(Constants.RETURN_TRUE)
                .endControlFlow()
                .addStatement(Constants.RETURN_FALSE)
                .build();

            results.add(keyReader);
            results.add(keyWriter);
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder("lockKey" + Util.upperFirst(composite.getKeyName()))
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Prevents any further updates to the key field.")
            .addStatement("keyLocked = true");
        results.add(builder.build());


        for (final PreprocessedNamedEiderObject compositeItem : composite.getObjectList())
        {
            results.add(
                MethodSpec.methodBuilder("copy" + Util.upperFirst(compositeItem.getName()) + "FromBuffer")
                    .addJavadoc("Copies " + compositeItem.getName() + " from the source to the buffer.")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ExpandableDirectByteBuffer.class, "sourceBuffer")
                    .addParameter(int.class, Constants.OFFSET)
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

            String flyWeight = compositeItem.getName().toUpperCase() + Constants.FLYWEIGHT;

            MethodSpec.Builder source = MethodSpec.methodBuilder("put" + Util.upperFirst(compositeItem.getName()))
                .addJavadoc("Copies the contents from source to this " + compositeItem.getName() + " buffer.")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("", Util.upperFirst(compositeItem.getObject().getName())), "source");

            List<PreprocessedEiderProperty> propertyList = compositeItem.getObject().getPropertyList();

            for (final PreprocessedEiderProperty property : propertyList)
            {
                final String prop = Util.upperFirst(property.getName());
                source.addStatement(flyWeight + ".write" + prop + "(source.read" + prop + "())");
            }

            results.add(source.build());

            results.add(
                MethodSpec.methodBuilder("get" + Util.upperFirst(compositeItem.getName()))
                    .addJavadoc("Returns the " + Util.upperFirst(compositeItem.getObject().getName()) + " flyweight.")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ClassName.get("", Util.upperFirst(compositeItem.getObject().getName())))
                    .addStatement("return " + compositeItem.getName().toUpperCase() + Constants.FLYWEIGHT)
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
                    compositeItem.getName().toUpperCase() + Constants.FLYWEIGHT)
                .addJavadoc("The flyweight for the " + compositeItem.getName() + " within this buffer.")
                .addModifiers(Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .initializer("new " + compositeItem.getObject().getName() + "()")
                .build());

            currentPos = currentPos + globalState.getBufferLengths().get(compositeItem.getObject().getName());
        }

        fields.add(FieldSpec
            .builder(ExpandableDirectByteBuffer.class, Constants.INTERNAL_BUFFER)
            .addJavadoc("The internal buffer to hold this composite object.")
            .addModifiers(Modifier.PRIVATE)
            .build());

        return fields;
    }

}

/*
 * Copyright 2019-2021 Shaun Laurens.
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

package io.eider.processor.agrona;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import io.eider.processor.EiderCodeWriter;
import io.eider.processor.PreprocessedEiderComposite;
import io.eider.processor.PreprocessedEiderObject;
import io.eider.processor.PreprocessedEiderRepeatableRecord;

import org.agrona.DirectBuffer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static io.eider.processor.agrona.Constants.BUFFER;
import static io.eider.processor.agrona.Constants.JAVA_NIO_BYTE_ORDER_LITTLE_ENDIAN1;
import static io.eider.processor.agrona.Constants.OFFSET;

public class AgronaWriter implements EiderCodeWriter
{
    private static final String IO_EIDER_UTIL = "io.eider.util";

    private final AgronaCompositeGenerator compositeGenerator = new AgronaCompositeGenerator();
    private final AgronaSpecGenerator specGenerator = new AgronaSpecGenerator();

    @Override
    public void generate(final ProcessingEnvironment pe,
                         final List<PreprocessedEiderRepeatableRecord> records,
                         final List<PreprocessedEiderObject> objects,
                         final List<PreprocessedEiderComposite> composites)
    {
        String packageName = null;

        final AgronaWriterGlobalState globalState = new AgronaWriterGlobalState();
        final List<PreprocessedEiderRepeatableRecord> alreadyGeneratedRecs = new ArrayList<>();

        for (final PreprocessedEiderObject object : objects)
        {
            if (specGenerator.hasAtLeastOneRecord(object))
            {
                List<PreprocessedEiderRepeatableRecord> requiredRecs = specGenerator.listRecords(object, records);
                if (requiredRecs.size() > 1)
                {
                    throw new RuntimeException("cannot have more than one repeated record at this time.");
                }
                for (PreprocessedEiderRepeatableRecord rec : requiredRecs)
                {
                    if (!alreadyGeneratedRecs.contains(rec))
                    {
                        //want the writing to be within the main object; this is just the basic outline
                        specGenerator.generateSpecRecord(pe, rec, globalState);
                        alreadyGeneratedRecs.add(rec);
                    }
                }
            }

            packageName = object.getPackageNameGen();
            AgronaWriterState state = new AgronaWriterState();
            specGenerator.generateSpecObject(pe, object, records, state, globalState);
            if (object.buildRepository())
            {
                specGenerator.generateSpecRepository(pe, object);
            }
        }

        if (packageName != null)
        {
            for (final PreprocessedEiderComposite composite : composites)
            {
                compositeGenerator.generateComposite(pe, composite, globalState);
                if (composite.buildRepository())
                {
                    compositeGenerator.generateCompositeRepository(pe, composite);
                }
            }
            generateEiderHelper(pe);
            generateEiderHelperInterfaces(pe);
            generateEiderHelperInterfaceForUnqiueIndex(pe);
        }
    }

    private void generateEiderHelperInterfaces(ProcessingEnvironment pe)
    {
        final String packageName = IO_EIDER_UTIL;

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("IndexUpdateConsumer")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(FunctionalInterface.class)
            .addTypeVariable(TypeVariableName.get("T")); //String, int, T

        MethodSpec acceptBuilder = MethodSpec.methodBuilder("accept")
            .addParameter(int.class, "offset")
            .addParameter(TypeVariableName.get("T"), "t")
            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
            .addJavadoc("Accepts an index update for the given offset and type<T> value")
            .build();

        builder.addMethod(acceptBuilder);

        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(packageName, generated)
            .build();

        try
        { // write the file
            JavaFileObject source = pe.getFiler()
                .createSourceFile(packageName + ".IndexUpdateConsumer");
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            //normal
        }
    }

    private void generateEiderHelperInterfaceForUnqiueIndex(ProcessingEnvironment pe)
    {
        final String packageName = IO_EIDER_UTIL;

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("IndexUniquenessConsumer")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(FunctionalInterface.class)
            .addTypeVariable(TypeVariableName.get("T")); //String, int, T

        MethodSpec uniqueBuilder = MethodSpec.methodBuilder("isUnique")
            .addParameter(TypeVariableName.get("T"), "t")
            .returns(boolean.class)
            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
            .addJavadoc("Accepts a type<T> value and checks that the index is valid")
            .build();

        builder.addMethod(uniqueBuilder);


        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(packageName, generated)
            .build();

        try
        { // write the file
            JavaFileObject source = pe.getFiler()
                .createSourceFile(packageName + ".IndexUniquenessConsumer");
            Writer writer = source.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            //normal
        }
    }

    private void generateEiderHelper(ProcessingEnvironment pe)
    {
        final String packageName = IO_EIDER_UTIL;

        TypeSpec.Builder builder = TypeSpec.classBuilder("EiderHelper")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethods(buildHeaderHelperMethods());
        TypeSpec generated = builder.build();

        JavaFile javaFile = JavaFile.builder(packageName, generated)
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
            //normal
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

}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import io.eider.annotation.EiderAttribute;
import io.eider.annotation.EiderRepository;
import io.eider.annotation.EiderSpec;

@SupportedAnnotationTypes( {
    "io.eider.annotation.EiderSpec"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class EiderAnnotationProcessor extends AbstractProcessor
{
    public static final String STRING = "java.lang.String";
    private int sequence = 0;
    private EiderCodeWriter writer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        writer = WriterFactory.getWriter(EiderGeneratorType.AGRONA);
        writeNote(processingEnv, "Eider is ready");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (roundEnv.processingOver() || annotations.isEmpty())
        {
            return false;
        }

        List<PreprocessedEiderObject> objects = new ArrayList<>();

        for (Element el : roundEnv.getElementsAnnotatedWith(EiderSpec.class))
        {
            boolean continueProcessing = false;
            if (el instanceof TypeElement)
            {
                continueProcessing = true;
            }

            if (!continueProcessing)
            {
                break;
            }

            TypeElement element = (TypeElement) el;
            preprocessObject(processingEnv, element, objects);
        }

        writer.generate(processingEnv, objects);
        writeNote(processingEnv, "Eider has finished");

        return true;
    }

    private void preprocessObject(ProcessingEnvironment processingEnv, TypeElement typeElement,
                                  final List<PreprocessedEiderObject> objects)
    {
        final String classNameInput = typeElement.getSimpleName().toString();
        final String classNameGen = classNameInput + "Eider";
        final String packageName = typeElement.getQualifiedName().toString();
        final String packageNameGen = packageName.replace(classNameInput, "gen");
        sequence += 1;
        writeNote(processingEnv, "Eider is preprocessing " + packageName + " - item: " + sequence);

        EiderSpec annotation = typeElement.getAnnotation(EiderSpec.class);

        final List<PreprocessedEiderProperty> preprocessedEiderProperties = new ArrayList<>();
        final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();

        for (Element element : enclosedElements)
        {
            ElementKind kind = element.getKind();
            if (kind == ElementKind.FIELD)
            {
                Map<String, String> annotations = new HashMap<>();
                boolean isFixed = false;
                EiderAttribute attribute = element.getAnnotation(EiderAttribute.class);
                if (attribute != null)
                {
                    annotations.put(Constants.MAXLENGTH, Integer.toString(attribute.maxLength()));
                    annotations.put(Constants.NULL_VALUE_LONG, Long.toString(attribute.nullValueLong()));
                    annotations.put(Constants.NULL_VALUE_STRING, attribute.nullValueString());
                    annotations.put(Constants.NULL_VALUE_INT, Integer.toString(attribute.nullValueInt()));
                    annotations.put(Constants.ATTRIBUTE_ORDER, Integer.toString(attribute.order()));
                    annotations.put(Constants.UNIQUE, Boolean.toString(attribute.indexed()));
                    annotations.put(Constants.INDEXED, Boolean.toString(attribute.unique()));
                    annotations.put(Constants.KEY, Boolean.toString(attribute.key()));

                    if (attribute.maxLength() != -1)
                    {
                        isFixed = true;
                    }
                }

                final String attrName = element.getSimpleName().toString();
                checkUnfixedStringInFixedObject(annotation, element, isFixed);

                final EiderPropertyType type = defineType(element.asType().toString(), isFixed);

                final PreprocessedEiderProperty prop = new PreprocessedEiderProperty(attrName, type, annotations);
                preprocessedEiderProperties.add(prop);
            }
        }

        final int objectEiderId;
        if (annotation.eiderId() == -1)
        {
            objectEiderId = sequence;
        }
        else
        {
            objectEiderId = annotation.eiderId();
        }

        final String name;
        if (!annotation.name().isEmpty())
        {
            name = annotation.name();
        }
        else
        {
            name = classNameGen;
        }

        EiderRepository repository = typeElement.getAnnotation(EiderRepository.class);
        final boolean enableRepository;
        final String repositoryName;
        if (repository == null)
        {
            enableRepository = false;
            repositoryName = "";
        }
        else
        {
            enableRepository = true;
            if (repository.name().isEmpty())
            {
                repositoryName = name + "Repository";
            }
            else
            {
                repositoryName = repository.name();
            }
        }

        final PreprocessedEiderObject obj = new PreprocessedEiderObject(name,
            classNameInput,
            objectEiderId,
            packageNameGen,
            annotation.fixedLength(),
            enableRepository,
            repositoryName,
            annotation.transactional(),
            preprocessedEiderProperties);

        objects.add(obj);
    }

    private EiderPropertyType defineType(String typeStr, boolean isFixed)
    {
        if (typeStr.equalsIgnoreCase(STRING) && isFixed)
        {
            return EiderPropertyType.FIXED_STRING;
        }
        else if (typeStr.equalsIgnoreCase(STRING) && !isFixed)
        {
            return EiderPropertyType.VAR_STRING;
        }

        return EiderPropertyType.from(typeStr);
    }

    private void checkUnfixedStringInFixedObject(final EiderSpec annotation, final Element element,
                                                 final boolean isFixed)
    {
        if (element.asType().toString().equalsIgnoreCase(STRING)
            &&
            !isFixed
            &&
            annotation.fixedLength())
        {
            throw new RuntimeException("Cannot have non fixed length strings on fixed length object");
        }
    }

    @Override
    public Set<String> getSupportedOptions()
    {
        final Set<String> options = new HashSet<>();
        return options;
    }

    private void writeNote(ProcessingEnvironment pe, String note)
    {
        pe.getMessager().printMessage(Diagnostic.Kind.NOTE, note);
    }

}

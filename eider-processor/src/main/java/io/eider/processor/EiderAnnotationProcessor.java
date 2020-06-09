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

import io.eider.annotation.EiderAttribute;
import io.eider.annotation.EiderComposite;
import io.eider.annotation.EiderRepository;
import io.eider.annotation.EiderSpec;

@SupportedAnnotationTypes( {
    "io.eider.annotation.EiderSpec",
    "io.eider.annotation.EiderComposite"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class EiderAnnotationProcessor extends AbstractProcessor
{
    public static final String STRING = "java.lang.String";
    private short sequence = 0;
    private EiderCodeWriter writer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        writer = WriterFactory.getWriter(EiderGeneratorType.AGRONA);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (roundEnv.processingOver() || annotations.isEmpty())
        {
            return false;
        }

        List<PreprocessedEiderObject> objects = new ArrayList<>();
        List<PreprocessedEiderComposite> composites = new ArrayList<>();

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
            preprocessObject(element, objects);
        }

        for (Element el : roundEnv.getElementsAnnotatedWith(EiderComposite.class))
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
            preprocessCompositeObject(processingEnv, element, objects, composites);
        }

        writer.generate(processingEnv, objects, composites);

        return true;
    }

    @SuppressWarnings("all")
    private void preprocessCompositeObject(ProcessingEnvironment pe, TypeElement typeElement,
                                           List<PreprocessedEiderObject> objects,
                                           List<PreprocessedEiderComposite> composites)
    {
        final String classNameInput = typeElement.getSimpleName().toString();
        final String packageName = typeElement.getQualifiedName().toString();
        final String packageNameGen = packageName.replace(classNameInput, "gen");
        String className = classNameInput + "EiderComposite";
        sequence += (short)1;
        int keyFieldCount = 0;

        EiderComposite annotation = typeElement.getAnnotation(EiderComposite.class);

        final List<PreprocessedNamedEiderObject> objectsIncluded = new ArrayList<>();
        final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();


        EiderPropertyType keyType = null;
        String keyName = "";

        if (!annotation.name().isEmpty())
        {
            className = annotation.name();
        }

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
                    annotations.put(Constants.KEY, Boolean.toString(attribute.key()));
                    if (attribute.key())
                    {
                        if (keyFieldCount != 0)
                        {
                            throw new EiderProcessorException("Only a single key field allowed");
                        }
                        keyFieldCount += 1;
                    }

                    if (attribute.maxLength() != -1)
                    {
                        isFixed = true;
                    }
                } else
                {
                    annotations.put(Constants.KEY, "false");
                }
                final String attrName = element.getSimpleName().toString();

                if (annotations.get(Constants.KEY).equalsIgnoreCase("true"))
                {
                    keyType = defineType(element.asType().toString(), isFixed);

                    if (keyType == EiderPropertyType.INVALID)
                    {
                        throw new EiderProcessorException("Only int and long fields can be EiderComposite keys");
                    }

                    keyName = attrName;
                }
                else
                {
                    PreprocessedEiderObject eiderObject = getEiderObject(element.asType().toString(), objects);

                    if (eiderObject == null)
                    {
                        throw new EiderProcessorException("Could not find eider spec for "
                            + element.asType().toString());
                    }
                    else
                    {
                        final PreprocessedNamedEiderObject cItem = new PreprocessedNamedEiderObject(attrName,
                            eiderObject);
                        objectsIncluded.add(cItem);
                    }
                }
            }
        }

        if (keyFieldCount == 0)
        {
            throw new EiderProcessorException("EiderComposite objects must have exactly 1 key");
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
                repositoryName = className + "Repository";
            }
            else
            {
                repositoryName = repository.name();
            }
        }

        short eiderId = annotation.eiderId() == -1 ? sequence : annotation.eiderId();
        final PreprocessedEiderComposite composite = new PreprocessedEiderComposite(className, classNameInput,
            eiderId, packageNameGen, enableRepository, repositoryName, keyName, keyType, objectsIncluded);
        composites.add(composite);
    }

    private PreprocessedEiderObject getEiderObject(String attrName,
                                                   List<PreprocessedEiderObject> objects)
    {

        final String[] f = attrName.split("\\.");
        final String typeNameNoPkg = f[f.length - 1];

        for (final PreprocessedEiderObject object : objects)
        {
            if (object.getClassNameInput().equalsIgnoreCase(typeNameNoPkg))
            {
                return object;
            }
        }
        return null;
    }

    @SuppressWarnings("all")
    private void preprocessObject(TypeElement typeElement,
                                  final List<PreprocessedEiderObject> objects)
    {
        final String classNameInput = typeElement.getSimpleName().toString();
        final String classNameGen = classNameInput + "Eider";
        final String packageName = typeElement.getQualifiedName().toString();
        final String packageNameGen = packageName.replace(classNameInput, "gen");
        sequence += (short)1;
        int keyFieldCount = 0;

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
                    annotations.put(Constants.KEY, Boolean.toString(attribute.key()));
                    annotations.put(Constants.SEQUENCE_GENERATOR, Boolean.toString(attribute.sequence()));

                    if (attribute.key())
                    {
                        if (keyFieldCount != 0)
                        {
                            throw new EiderProcessorException("Only a single key field allowed");
                        }
                        keyFieldCount += 1;
                    }

                    if (attribute.maxLength() != -1)
                    {
                        isFixed = true;
                    }
                }
                else
                {
                    applyDefaultAnnotations(annotations);
                }

                final String attrName = element.getSimpleName().toString();
                checkUnfixedStringInFixedObject(annotation, element, isFixed);

                final EiderPropertyType type = defineType(element.asType().toString(), isFixed);

                final PreprocessedEiderProperty prop = new PreprocessedEiderProperty(attrName, type, annotations);
                preprocessedEiderProperties.add(prop);
            }
        }

        final short objectEiderId;
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
        final boolean enableTransactionalRepository;
        final String repositoryName;
        if (repository == null)
        {
            enableRepository = false;
            enableTransactionalRepository = false;
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
            enableTransactionalRepository = repository.transactional();
        }

        final PreprocessedEiderObject obj = new PreprocessedEiderObject(name,
            classNameInput,
            objectEiderId,
            annotation.eiderGroup(),
            packageNameGen,
            annotation.fixedLength(),
            enableRepository,
            repositoryName,
            annotation.transactional(),
            enableTransactionalRepository,
            preprocessedEiderProperties);

        objects.add(obj);
    }

    private void applyDefaultAnnotations(Map<String, String> annotations)
    {
        annotations.put(Constants.MAXLENGTH, Integer.toString(Integer.MIN_VALUE));
        annotations.put(Constants.SEQUENCE_GENERATOR, Boolean.toString(false));
        annotations.put(Constants.KEY, Boolean.toString(false));
    }

    private EiderPropertyType defineType(String typeStr, boolean isFixed)
    {
        if (typeStr.equalsIgnoreCase(STRING) && isFixed)
        {
            return EiderPropertyType.FIXED_STRING;
        }
        else if (typeStr.equalsIgnoreCase(STRING) && !isFixed)
        {
            return EiderPropertyType.INVALID;
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
            throw new EiderProcessorException("Cannot have non fixed length strings on fixed length object");
        }
    }

}

/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sourcegen.bytecode;

import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.EnumDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.TypeDef;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The enum generator utils.
 *
 * @author Denis Stepanov
 * @since 1.5
 */
public class EnumGenUtils {

    private static final java.lang.reflect.Method CLONE_METHOD = ReflectionUtils.getRequiredMethod(Object.class, "clone");

    /**
     * Generate the {@link ClassDef} from {@link EnumDef}.
     *
     * @param enumDef The enum def
     * @return The class definition
     */
    public static ClassDef toClassDef(EnumDef enumDef) {
        ClassTypeDef enumTypeDef = enumDef.asTypeDef();

        ClassTypeDef baseEnumTypeDef = ClassTypeDef.of(Enum.class);

        ClassDef.ClassDefBuilder classDefBuilder = ClassDef.builder(enumDef.getName())
            .addModifiers(enumDef.getModifiers())
            .addModifiers(Modifier.FINAL)
            .superclass(TypeDef.parameterized(baseEnumTypeDef, ClassTypeDef.of(enumDef.getName())))
            .addSuperinterfaces(enumDef.getSuperinterfaces());

        int i = 0;
        for (Map.Entry<String, List<ExpressionDef>> e : enumDef.getEnumConstants().entrySet()) {

            FieldDef enumField = FieldDef.builder(e.getKey(), enumTypeDef)
                .addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                .initializer(enumTypeDef.instantiate(
                    ExpressionDef.constant(e.getKey()),
                    TypeDef.Primitive.INT.constant(i++)
                ))
                .build();

            classDefBuilder.addField(enumField);
        }

        classDefBuilder.addMethod(
            MethodDef.constructor()
                .addParameter(ParameterDef.of("name", TypeDef.STRING))
                .addParameter(ParameterDef.of("ordinal", TypeDef.Primitive.INT))
                .build((aThis, methodParameters) -> aThis.superRef().invokeConstructor(methodParameters.get(0), methodParameters.get(1)))
        );

        MethodDef internalValuesMethod = MethodDef.builder("$values")
            .addModifiers(Modifier.STATIC, Modifier.PRIVATE)
            .build((aThis, methodParameters) ->
                enumTypeDef.array()
                    .instantiate(
                        enumDef.getEnumConstants()
                            .keySet()
                            .stream()
                            .<ExpressionDef>map(name -> enumTypeDef.getStaticField(name, enumTypeDef))
                            .toList()
                    )
                    .returning());

        classDefBuilder.addMethod(internalValuesMethod);

        FieldDef valuesField = FieldDef.builder("$VALUES").ofType(enumTypeDef.array())
            .addModifiers(Modifier.STATIC, Modifier.PRIVATE)
            .initializer(enumTypeDef.invokeStatic(internalValuesMethod))
            .build();

        classDefBuilder.addField(valuesField);

        classDefBuilder.addMethod(MethodDef.builder("values")
            .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
            .returns(enumTypeDef.array())
            .build((aThis2, methodParameters2) ->
                enumTypeDef.getStaticField(valuesField)
                    .invoke(CLONE_METHOD)
                    .cast(enumTypeDef.array())
                    .returning()));

        classDefBuilder.addMethod(MethodDef.builder("valueOf")
            .addParameter(ParameterDef.of("value", TypeDef.STRING))
            .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
            .build((aThis1, methodParameters1) ->
                baseEnumTypeDef
                    .invokeStatic("valueOf", baseEnumTypeDef, ExpressionDef.constant(enumTypeDef), methodParameters1.get(0))
                    .cast(enumTypeDef)
                    .returning()));

        classDefBuilder.addMethods(enumDef.getMethods());

        return classDefBuilder.build();
    }

    /**
     * Is enum field.
     *
     * @param objectDef The object def
     * @param fieldDef  The field
     * @return true if is an enum field
     */
    public static boolean isEnumField(ObjectDef objectDef, FieldDef fieldDef) {
        Optional<ExpressionDef> initializer = fieldDef.getInitializer();
        return objectDef instanceof ClassDef classDef && isEnum(classDef)
            && initializer.isPresent()
            && initializer.get() instanceof ExpressionDef.NewInstance ni
            && ni.type().getName().equals(classDef.getName());
    }

    /**
     * Is enum class.
     *
     * @param classDef The class def
     * @return true if the enum class
     */
    public static boolean isEnum(ClassDef classDef) {
        return classDef.getSuperclass() != null && classDef.getSuperclass().getName().equals(Enum.class.getName());
    }

}

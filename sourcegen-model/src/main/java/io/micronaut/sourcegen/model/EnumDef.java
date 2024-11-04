/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.sourcegen.model;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;

import static java.lang.String.join;

/**
 * The enum definition.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class EnumDef extends ObjectDef {

    private final List<FieldDef> fields;
    private final LinkedHashMap<String, ExpressionDef> enumConstants;

    private EnumDef(String name,
                    EnumSet<Modifier> modifiers,
                    List<FieldDef> fields,
                    List<MethodDef> methods,
                    List<PropertyDef> properties,
                    List<AnnotationDef> annotations,
                    List<String> javadoc,
                    LinkedHashMap<String, ExpressionDef> enumConstants,
                    List<TypeDef> superinterfaces,
                    List<ObjectDef> innerTypes) {
        super(name, modifiers, annotations, javadoc, methods, properties, superinterfaces, innerTypes);
        this.fields = fields;
        this.enumConstants = enumConstants;
    }

    public static EnumDefBuilder builder(String name) {
        return new EnumDefBuilder(name);
    }

    public List<FieldDef> getFields() {
        return fields;
    }

    public LinkedHashMap<String, ExpressionDef> getEnumConstants() {
        return enumConstants;
    }

    @Nullable
    public FieldDef findField(String name) {
        for (FieldDef field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        for (PropertyDef property : getProperties()) {
            if (property.getName().equals(name)) {
                return FieldDef.builder(property.getName()).ofType(property.getType()).build();
            }
        }
        return null;
    }

    @NonNull
    public FieldDef getField(String name) {
        FieldDef field = findField(name);
        if (field == null) {
            throw new IllegalStateException("Enum: " + this.name + " doesn't have a field: " + name);
        }
        return null;
    }

    public boolean hasField(String name) {
        FieldDef property = findField(name);
        if (property != null) {
            return true;
        }
        // TODO: check outer classes
        return false;
    }

    /**
     * The enum definition builder.
     *
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    public static final class EnumDefBuilder extends ObjectDefBuilder<EnumDefBuilder> {

        private final List<FieldDef> fields = new ArrayList<>();
        private final LinkedHashMap<String, ExpressionDef> enumConstants = new LinkedHashMap<>();

        private EnumDefBuilder(String name) {
            super(name);
        }

        public EnumDefBuilder addField(FieldDef field) {
            fields.add(field);
            return this;
        }

        public EnumDefBuilder addEnumConstant(String name) {
            String constName = getConstantName(name);
            enumConstants.put(constName, null);
            return this;
        }

        public EnumDefBuilder addEnumConstant(String name, ExpressionDef value) {
            String constName = getConstantName(name);
            enumConstants.put(constName, value);
            return this;
        }

        public EnumDef build() {
            return new EnumDef(name, modifiers, fields, methods, properties, annotations, javadoc, enumConstants, superinterfaces, innerTypes);
        }

        /**
         * Add a constructor.
         *
         * @param parameterDefs The fields to set in the constructor
         * @param modifiers The method modifiers
         * @return this
         */
        public EnumDefBuilder addConstructor(Collection<ParameterDef> parameterDefs, Modifier... modifiers) {
            return this.addMethod(
                MethodDef.constructor(ClassTypeDef.of(name), parameterDefs, modifiers)
            );
        }

        /**
         * Add a constructor for all fields and property.
         *
         * @param modifiers The modifiers
         * @return this
         */
        public EnumDefBuilder addAllFieldsConstructor(Modifier... modifiers) {
            List<ParameterDef> constructorParameters = new ArrayList<>();
            for (PropertyDef property : properties) {
                constructorParameters.add(ParameterDef.of(property.getName(), property.getType()));
            }
            for (FieldDef field: fields) {
                constructorParameters.add(ParameterDef.of(field.getName(), field.getType()));
            }
            return this.addMethod(
                MethodDef.constructor(ClassTypeDef.of(name), constructorParameters, modifiers)
            );
        }

        /**
         * Add a constructor with no arguments.
         *
         * @param modifiers The method modifiers
         * @return this
         */
        public EnumDefBuilder addNoFieldsConstructor(Modifier... modifiers) {
            return this.addMethod(
                MethodDef.constructor(ClassTypeDef.of(name), Collections.emptyList(), modifiers)
            );
        }

        private static String getConstantName(String input) {
            if (input.equals(input.toUpperCase())) {
                return input;
            }
            String cleanedInput = input.replaceAll("[-_]", " ")
                .replaceAll("(?<!^)(?=[A-Z])", " ")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .trim();

            while (!Character.isJavaIdentifierStart(cleanedInput.charAt(0))) {
                cleanedInput = cleanedInput.substring(1);
            }

            // Split into words
            String[] words = cleanedInput.split("\\s+");

            // Check if the input is acceptable
            if (words.length == 0 || words[0].isEmpty()) {
                throw new IllegalArgumentException("Property name is not an acceptable enum constant name");
            }
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].toUpperCase();
            }
            return join("_", words);
        }

    }

}

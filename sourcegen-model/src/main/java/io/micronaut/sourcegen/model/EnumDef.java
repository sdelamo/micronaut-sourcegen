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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private final LinkedHashMap<String, List<ExpressionDef>> enumConstants;

    private EnumDef(ClassTypeDef type,
                    EnumSet<Modifier> modifiers,
                    List<FieldDef> fields,
                    List<MethodDef> methods,
                    List<PropertyDef> properties,
                    List<AnnotationDef> annotations,
                    List<String> javadoc,
                    LinkedHashMap<String, List<ExpressionDef>> enumConstants,
                    List<TypeDef> superinterfaces,
                    List<ObjectDef> innerTypes) {
        super(type, modifiers, annotations, javadoc, methods, properties, superinterfaces, innerTypes);
        this.fields = fields;
        this.enumConstants = enumConstants;
    }

    @Override
    public EnumDef withType(ClassTypeDef type) {
        return new EnumDef(type, modifiers, fields, methods, properties, annotations, javadoc, enumConstants, superinterfaces, innerTypes);
    }

    public static EnumDefBuilder builder(String name) {
        return new EnumDefBuilder(name);
    }

    public List<FieldDef> getFields() {
        return fields;
    }

    public LinkedHashMap<String, List<ExpressionDef>> getEnumConstants() {
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
        return property != null;
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
        private final LinkedHashMap<String, List<ExpressionDef>> enumConstants = new LinkedHashMap<>();

        private EnumDefBuilder(String name) {
            super(name);
        }

        public EnumDefBuilder addField(FieldDef field) {
            fields.add(field);
            return this;
        }

        public EnumDefBuilder addEnumConstant(String name) {
            String constName = getConstantName(name);
            enumConstants.put(constName, List.of());
            return this;
        }

        public EnumDefBuilder addEnumConstant(String name, ExpressionDef... values) {
            Objects.requireNonNull(values, "Values cannot be null");
            String constName = getConstantName(name);
            enumConstants.put(constName, List.of(values));
            return this;
        }

        public EnumDef build() {
            if (!enumConstants.isEmpty()) {
                Set<Integer> valueCount = new HashSet<>();
                for (Map.Entry<String, List<ExpressionDef>> entry : enumConstants.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        continue;
                    }

                    int constCount = entry.getValue().size();
                    if (valueCount.contains(constCount)) {
                        continue;
                    } else {
                        valueCount.add(constCount);
                    }

                    boolean hasConstructor = false;
                    for (MethodDef methodDef: methods) {
                        if (methodDef.isConstructor() && methodDef.getParameters().size() == constCount) {
                            hasConstructor = true;
                        }
                        if (methodDef.isConstructor() && !methodDef.getModifiers().contains(Modifier.PRIVATE)) {
                            throw new IllegalStateException("The constructor of enum: " + name + " has to be private.");
                        }
                    }
                    if (!hasConstructor) {
                        throw new IllegalStateException("Enum: " + name + " doesn't have a constructor for constant " + entry.getKey());
                    }
                }
            }
            return new EnumDef(ClassTypeDef.of(name), modifiers, fields, methods, properties, annotations, javadoc, enumConstants, superinterfaces, innerTypes);
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
                MethodDef.constructor(parameterDefs, modifiers)
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
                MethodDef.constructor(constructorParameters, modifiers)
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
                MethodDef.constructor(Collections.emptyList(), modifiers)
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
            try {
                // Check if the input is acceptable
                if (words.length == 0 || words[0].isEmpty()) {
                    throw new IllegalArgumentException("The enum constant name is not an acceptable identifier name.");
                }
                for (int i = 0; i < words.length; i++) {
                    words[i] = words[i].toUpperCase();
                }
                String constantName = join("_", words);

                if (!constantName.equals(input)) {
                    throw new IllegalArgumentException("The enum constant name does not follow the conventions for constants, it should be changed accordingly.");
                }
                return constantName;
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }

    }

}

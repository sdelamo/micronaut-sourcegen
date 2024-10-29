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
import io.micronaut.core.annotation.Nullable;

import javax.lang.model.element.Modifier;
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

    private final LinkedHashMap<String, ExpressionDef> enumConstants;

    private EnumDef(String name,
                    EnumSet<Modifier> modifiers,
                    List<MethodDef> methods,
                    List<PropertyDef> properties,
                    List<AnnotationDef> annotations,
                    List<String> javadoc,
                    LinkedHashMap<String, ExpressionDef> enumConstants,
                    List<TypeDef> superinterfaces) {
        super(name, modifiers, annotations, javadoc, methods, properties, superinterfaces);
        this.enumConstants = enumConstants;
    }

    public static EnumDefBuilder builder(String name) {
        return new EnumDefBuilder(name);
    }

    public LinkedHashMap<String, ExpressionDef> getEnumConstants() {
        return enumConstants;
    }

    @Nullable
    public PropertyDef findProperty(String name) {
        for (PropertyDef property : getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    public boolean hasProperty(String name) {
        PropertyDef property = findProperty(name);
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

        private final LinkedHashMap<String, ExpressionDef> enumConstants = new LinkedHashMap<>();

        private EnumDefBuilder(String name) {
            super(name);
        }

        public EnumDefBuilder addEnumConstant(String name) {
            String constName = getConstantName(name);
            if (!constName.equals(name)) {
                return addEnumConstant(constName, ExpressionDef.constant(name));
            }
            enumConstants.put(name, null);
            return this;
        }

        public EnumDefBuilder addEnumConstant(String name, ExpressionDef value) {
            String constName = getConstantName(name);
            enumConstants.put(constName, value);
            return this;
        }

        public EnumDef build() {
            return new EnumDef(name, modifiers, methods, properties, annotations, javadoc, enumConstants, superinterfaces);
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

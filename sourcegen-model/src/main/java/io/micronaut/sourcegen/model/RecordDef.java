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

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The class definition.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class RecordDef extends ObjectDef {

    private final List<TypeDef.TypeVariable> typeVariables;

    private RecordDef(ClassTypeDef type,
                      EnumSet<Modifier> modifiers,
                      List<MethodDef> methods,
                      List<PropertyDef> properties,
                      List<AnnotationDef> annotations,
                      List<String> javadoc,
                      List<TypeDef.TypeVariable> typeVariables,
                      List<TypeDef> superinterfaces,
                      List<ObjectDef> innerTypes) {
        super(type, modifiers, annotations, javadoc, methods, properties, superinterfaces, innerTypes);
        this.typeVariables = typeVariables;
    }

    @Override
    public RecordDef withType(ClassTypeDef type) {
        return new RecordDef(type, modifiers, methods, properties, annotations, javadoc, typeVariables, superinterfaces, innerTypes);
    }

    public static RecordDefBuilder builder(String name) {
        return new RecordDefBuilder(name);
    }

    public List<TypeDef.TypeVariable> getTypeVariables() {
        return typeVariables;
    }

    /**
     * The record definition builder.
     *
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    public static final class RecordDefBuilder extends ObjectDefBuilder<RecordDefBuilder> {

        private final List<TypeDef.TypeVariable> typeVariables = new ArrayList<>();

        private RecordDefBuilder(String name) {
            super(name);
        }

        public RecordDefBuilder addTypeVariable(TypeDef.TypeVariable typeVariable) {
            typeVariables.add(typeVariable);
            return this;
        }

        public RecordDef build() {
            return new RecordDef(ClassTypeDef.of(name), modifiers, methods, properties, annotations, javadoc, typeVariables, superinterfaces, innerTypes);
        }

    }

}

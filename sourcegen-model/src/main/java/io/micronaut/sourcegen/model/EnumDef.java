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

import com.github.javaparser.utils.Pair;
import io.micronaut.core.annotation.Experimental;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The enum definition.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class EnumDef extends ObjectDef {

    private final List<Pair<String,Object>> enumConstants;

    private EnumDef(String name,
                    EnumSet<Modifier> modifiers,
                    List<MethodDef> methods,
                    List<AnnotationDef> annotations,
                    List<String> javadoc,
                    List<Pair<String,Object>> enumConstants,
                    List<TypeDef> superinterfaces) {
        super(name, modifiers, annotations, javadoc, methods, superinterfaces);
        this.enumConstants = enumConstants;
    }

    public static EnumDefBuilder builder(String name) {
        return new EnumDefBuilder(name);
    }

    public List<Pair<String,Object>> getEnumConstants() {
        return enumConstants;
    }

    /**
     * The enum definition builder.
     *
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    public static final class EnumDefBuilder extends ObjectDefBuilder<EnumDefBuilder> {

        private final List<Pair<String,Object>> enumConstants = new ArrayList<>();

        private EnumDefBuilder(String name) {
            super(name);
        }

        public EnumDefBuilder addEnumConstant(String name) {
            enumConstants.add(new Pair<>(name,null));
            return this;
        }

        public EnumDefBuilder addEnumConstant(String name, Object value) {
            enumConstants.add(new Pair<>(name,value));
            return this;
        }

        public EnumDef build() {
            return new EnumDef(name, modifiers, methods, annotations, javadoc, enumConstants, superinterfaces);
        }

    }

}

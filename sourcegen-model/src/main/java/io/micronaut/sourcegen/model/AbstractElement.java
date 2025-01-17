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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The abstract element.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
abstract sealed class AbstractElement permits ObjectDef, FieldDef, MethodDef, ParameterDef, PropertyDef {

    protected final String name;
    protected final EnumSet<Modifier> modifiers;
    protected final List<AnnotationDef> annotations;
    protected final List<String> javadoc;
    protected final boolean synthetic;

    AbstractElement(String name, EnumSet<Modifier> modifiers, List<AnnotationDef> annotations, List<String> javadoc, boolean synthetic) {
        this.name = name;
        this.modifiers = modifiers;
        this.annotations = Collections.unmodifiableList(annotations);
        this.javadoc = Collections.unmodifiableList(javadoc);
        this.synthetic = synthetic;
    }

    /**
     * @return Is synthetic element
     */
    public boolean isSynthetic() {
        return synthetic;
    }

    public final String getName() {
        return name;
    }

    public final Set<Modifier> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    public final Modifier[] getModifiersArray() {
        return modifiers.toArray(Modifier[]::new);
    }

    public final List<AnnotationDef> getAnnotations() {
        return annotations;
    }

    public List<String> getJavadoc() {
        return javadoc;
    }
}

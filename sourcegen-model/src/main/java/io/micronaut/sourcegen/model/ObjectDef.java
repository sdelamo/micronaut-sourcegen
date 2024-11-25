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
import io.micronaut.core.naming.NameUtils;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;

/**
 * The abstract class representing a type: class, enum, interface or record.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public abstract sealed class ObjectDef extends AbstractElement permits ClassDef, EnumDef, InterfaceDef, RecordDef {

    private final List<MethodDef> methods;
    private final List<PropertyDef> properties;
    private final List<TypeDef> superinterfaces;
    private final List<ObjectDef> innerTypes;

    ObjectDef(
        String name, Set<Modifier> modifiers, List<AnnotationDef> annotations,
        List<String> javadoc, List<MethodDef> methods, List<PropertyDef> properties,
        List<TypeDef> superinterfaces,
        List<ObjectDef> innerTypes
    ) {
        super(name, modifiers, annotations, javadoc);
        this.methods = methods;
        this.properties = properties;
        this.superinterfaces = superinterfaces;
        this.innerTypes = innerTypes;
    }

    public final List<MethodDef> getMethods() {
        return methods;
    }

    public final List<PropertyDef> getProperties() {
        return properties;
    }

    public final List<TypeDef> getSuperinterfaces() {
        return superinterfaces;
    }

    public final String getPackageName() {
        return NameUtils.getPackageName(getName());
    }

    public final String getSimpleName() {
        return NameUtils.getSimpleName(getName());
    }

    public final List<ObjectDef> getInnerTypes() {
        return innerTypes;
    }

    /**
     * Get the type definition for this type.
     *
     * @return The type definition
     */
    public ClassTypeDef asTypeDef() {
        return ClassTypeDef.of(getName());
    }

    /**
     * Get the actual contextual type.
     *
     * @param typeDef The type
     * @return The contextual type or original type
     * @since 1.5
     */
    @NonNull
    public TypeDef getContextualType(@NonNull TypeDef typeDef) {
        if (typeDef == TypeDef.THIS) {
            return asTypeDef();
        } else if (typeDef == TypeDef.SUPER) {
            if (this instanceof ClassDef classDef) {
                if (classDef.getSuperclass() == null) {
                    return TypeDef.of(Object.class);
                }
                return classDef.getSuperclass();
            } else if (this instanceof EnumDef) {
                return ClassTypeDef.of(Enum.class);
            } else if (this instanceof InterfaceDef interfaceDef) {
                throw new IllegalStateException("Super class is not supported for interface def: " + interfaceDef);
            }
        }
        return typeDef;
    }

    /**
     * Get a contextual type (converts this or super type to appropriate one).
     *
     * @param objectDef The object def
     * @param typeDef   The type def
     * @return the contextual type or type def provider
     * @since 1.4
     */
    @NonNull
    public static TypeDef getContextualType(@Nullable ObjectDef objectDef, @NonNull TypeDef typeDef) {
        if (objectDef == null) {
            if ((typeDef == TypeDef.THIS || typeDef == TypeDef.SUPER)) {
                throw new IllegalStateException("Cannot determine type: " + typeDef + " because object def is null");
            }
            return typeDef;
        }
        return objectDef.getContextualType(typeDef);
    }

}

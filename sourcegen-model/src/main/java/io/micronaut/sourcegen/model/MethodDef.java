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
import io.micronaut.inject.ast.MethodElement;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The method definition.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class MethodDef extends AbstractElement {

    public static final String CONSTRUCTOR = "<init>";
    private final TypeDef returnType;
    private final List<ParameterDef> parameters;
    private final List<StatementDef> statements;
    private final boolean override;

    MethodDef(String name,
              EnumSet<Modifier> modifiers,
              TypeDef returnType,
              List<ParameterDef> parameters,
              List<StatementDef> statements,
              List<AnnotationDef> annotations,
              List<String> javadoc,
              boolean override) {
        super(name, modifiers, annotations, javadoc);
        this.returnType = Objects.requireNonNullElse(returnType, TypeDef.VOID);
        this.parameters = Collections.unmodifiableList(parameters);
        this.statements = statements;
        this.override = override;
    }

    /**
     * @return Starts a constructor.
     */
    public static MethodDefBuilder constructor() {
        return MethodDef.builder(CONSTRUCTOR);
    }

    /**
     * Create a new constructor with parameters assigned to fields with the same name.
     *
     * @param parameterDefs The parameters of the body
     * @param modifiers     The constructor modifiers
     * @return A new constructor with a body.
     */
    public static MethodDef constructor(Collection<ParameterDef> parameterDefs, Modifier... modifiers) {
        MethodDefBuilder builder = MethodDef.builder(CONSTRUCTOR);
        int paramIndex = 0;
        for (ParameterDef parameterDef : parameterDefs) {
            builder.addParameter(parameterDef);
            int finalParamIndex = paramIndex;
            builder.addStatement((aThis, methodParameters) -> aThis.field(parameterDef.getName(), parameterDef.getType())
                .put(methodParameters.get(finalParamIndex)));
            paramIndex++;
        }
        builder.addModifiers(modifiers);
        return builder.build();
    }

    public static MethodDef of(MethodElement methodElement) {
        return MethodDef.builder(methodElement.getName())
            .addParameters(Arrays.stream(methodElement.getSuspendParameters()).map(p -> TypeDef.erasure(p.getType())).toList())
            .returns(methodElement.isSuspend() ? TypeDef.OBJECT : TypeDef.erasure(methodElement.getReturnType()))
            .build();
    }

    public static MethodDef of(Method method) {
        return MethodDef.builder(method.getName())
            .addParameters(Arrays.stream(method.getParameters()).map(p -> TypeDef.of(p.getType())).toList())
            .returns(TypeDef.of(method.getReturnType()))
            .build();
    }

    public static MethodDefBuilder override(MethodElement methodElement) {
        return MethodDef.builder(methodElement.getName())
            .addModifiers(toOverrideModifiers(methodElement))
            .addParameters(Arrays.stream(methodElement.getSuspendParameters()).map(p -> TypeDef.erasure(p.getType())).toList())
            .returns(methodElement.isSuspend() ? TypeDef.OBJECT : TypeDef.erasure(methodElement.getReturnType()));
    }

    public static MethodDefBuilder override(Method method) {
        return MethodDef.builder(method.getName())
            .addModifiers(toOverrideModifiers(method.getModifiers()))
            .addParameters(Arrays.stream(method.getParameters()).map(p -> TypeDef.of(p.getType())).toList())
            .returns(TypeDef.of(method.getReturnType()));
    }

    private static Modifier[] toOverrideModifiers(int modifiers) {
        List<Modifier> modifiersList = new ArrayList<>();
        if (java.lang.reflect.Modifier.isPublic(modifiers)) {
            modifiersList.add(Modifier.PUBLIC);
        }
        if (java.lang.reflect.Modifier.isProtected(modifiers)) {
            modifiersList.add(Modifier.PROTECTED);
        }
        return modifiersList.toArray(new Modifier[0]);
    }

    private static Modifier[] toOverrideModifiers(MethodElement methodElement) {
        List<Modifier> modifiersList = new ArrayList<>();
        if (methodElement.isPublic()) {
            modifiersList.add(Modifier.PUBLIC);
        }
        if (methodElement.isProtected()) {
            modifiersList.add(Modifier.PROTECTED);
        }
        return modifiersList.toArray(new Modifier[0]);
    }

    public boolean isDefaultConstructor() {
        return CONSTRUCTOR.equals(getName()) && parameters.isEmpty();
    }

    public TypeDef getReturnType() {
        return returnType;
    }

    public List<ParameterDef> getParameters() {
        return parameters;
    }

    public List<StatementDef> getStatements() {
        return statements;
    }

    @Nullable
    public ParameterDef findParameter(String name) {
        for (ParameterDef parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    @NonNull
    public ParameterDef getParameter(String name) {
        ParameterDef parameter = findParameter(name);
        if (parameter == null) {
            throw new IllegalStateException("Method: " + name + " doesn't have parameter: " + name);
        }
        return parameter;
    }

    /**
     * @return True if method is an override
     */
    public boolean isOverride() {
        return override;
    }

    /**
     * @return True if method is a constructor
     */
    public boolean isConstructor() {
        return CONSTRUCTOR.equals(getName());
    }

    public static MethodDefBuilder builder(String name) {
        return new MethodDefBuilder(name);
    }

    @Override
    public String toString() {
        return "MethodDef{" +
            "name='" + name + '\'' +
            ", modifiers=" + modifiers +
            ", returnType=" + returnType +
            ", parameters=" + parameters +
            ", statements=" + statements +
            ", override=" + override +
            '}';
    }

    /**
     * The method builder definition.
     *
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    public static final class MethodDefBuilder extends AbstractElementBuilder<MethodDefBuilder> {

        private final List<ParameterDef> parameters = new ArrayList<>();
        private TypeDef returnType;
        private final List<BodyBuilder> bodyBuilders = new ArrayList<>();
        private final List<StatementDef> statements = new ArrayList<>();
        private boolean overrides;

        private MethodDefBuilder(String name) {
            super(name);
        }

        /**
         * The return type of the method.
         * In a case of missing return type it will be extracted from the statements.
         *
         * @param type The return type
         * @return the current builder
         */
        public MethodDefBuilder returns(TypeDef type) {
            this.returnType = type;
            return this;
        }

        /**
         * Mark the method as an override.
         *
         * @return the current builder
         */
        public MethodDefBuilder overrides() {
            return overrides(true);
        }

        /**
         * Mark the method as an override.
         *
         * @param overrides The value
         * @return the current builder
         */
        public MethodDefBuilder overrides(boolean overrides) {
            this.overrides = overrides;
            return this;
        }

        public MethodDefBuilder returns(Class<?> type) {
            return returns(TypeDef.of(type));
        }

        public MethodDefBuilder addParameter(String name, TypeDef type) {
            ParameterDef parameterDef = ParameterDef.builder(name, type).build();
            return addParameter(parameterDef);
        }

        public MethodDefBuilder addParameter(TypeDef type) {
            return addParameter("parameter" + (parameters.size() + 1), type);
        }

        public MethodDefBuilder addParameter(ParameterDef parameterDef) {
            Objects.requireNonNull(parameterDef, "Parameter cannot be null");
            parameters.add(parameterDef);
            return this;
        }

        public MethodDefBuilder addParameter(String name, Class<?> type) {
            return addParameter(name, TypeDef.of(type));
        }

        public MethodDefBuilder addParameter(Class<?> type) {
            return addParameter(TypeDef.of(type));
        }

        public MethodDefBuilder addParameters(Class<?>... types) {
            for (Class<?> type : types) {
                addParameter(type);
            }
            return this;
        }

        public MethodDefBuilder addParameters(TypeDef... types) {
            return addParameters(List.of(types));
        }

        public MethodDefBuilder addParameters(List<TypeDef> types) {
            for (TypeDef type : types) {
                addParameter(type);
            }
            return this;
        }

        public MethodDefBuilder addStaticStatement(Function<List<VariableDef.MethodParameter>, StatementDef> bodyBuilder) {
            return addStatement((aThis, methodParameters) -> bodyBuilder.apply(methodParameters));
        }

        public MethodDefBuilder addStatement(StatementDef statement) {
            if (statement instanceof StatementDef.Multi multi) {
                multi.statements().forEach(this::addStatement);
            } else {
                statements.add(statement);
            }
            return this;
        }

        public MethodDefBuilder addStatement(BodyBuilder bodyBuilder) {
            bodyBuilders.add(bodyBuilder);
            return this;
        }

        public MethodDefBuilder addStatements(Collection<StatementDef> newStatements) {
            statements.addAll(newStatements);
            return this;
        }

        public MethodDef build() {
            List<VariableDef.MethodParameter> variables = parameters.stream()
                .map(ParameterDef::asVariable)
                .toList();
            for (BodyBuilder bodyBuilder : bodyBuilders) {
                StatementDef statement = bodyBuilder.apply(new VariableDef.This(), variables);
                if (statement != null) {
                    addStatement(statement);
                }
            }
            if (returnType == null && !statements.isEmpty()) {
                StatementDef lastStatement = statements.get(statements.size() - 1);
                if (lastStatement instanceof StatementDef.Return aReturn) {
                    returnType = aReturn.expression().type();
                }
                if (lastStatement instanceof StatementDef.Try aTry) {
                    returnType = findReturnType(aTry);
                }
            }
            if (returnType == null && !name.equals(CONSTRUCTOR)) {
                returnType = TypeDef.VOID;
            }
            return new MethodDef(name, modifiers, returnType, parameters, statements, annotations, javadoc, overrides);
        }

        private TypeDef findReturnType(StatementDef statement) {
            if (statement instanceof StatementDef.Return aReturn) {
                return aReturn.expression().type();
            }
            if (statement instanceof StatementDef.Try aTry) {
                return findReturnType(aTry.statement());
            }
            return null;
        }

        public MethodDef build(BodyBuilder bodyBuilder) {
            bodyBuilders.add(bodyBuilder);
            return build();
        }

        public MethodDef buildStatic(Function<List<VariableDef.MethodParameter>, StatementDef> bodyBuilder) {
            modifiers.add(Modifier.STATIC);
            bodyBuilders.add((aThis, methodParameters) -> bodyBuilder.apply(methodParameters));
            return build();
        }

    }

    /**
     * The body builder.
     *
     * @author Denis Stepanov
     * @since 1.4
     */
    public interface BodyBuilder extends BiFunction<VariableDef.This, List<VariableDef.MethodParameter>, StatementDef> {
    }
}

package com.skydoves.processor;

import android.support.annotation.NonNull;

import com.google.common.base.VerifyException;
import com.skydoves.preferenceroom.InjectPreference;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Developed by skydoves on 2017-11-23.
 * Copyright (c) 2017 skydoves rights reserved.
 */

public class InjectorGenerator {

    private static final String CLAZZ_PREFIX = "Injector_";
    private static final String INJECT_OBJECT = "injectObject";
    private static final String PREFERENCE_PREFIX = "Preference_";
    private static final String COMPONENT_PREFIX = "PreferenceComponent_";

    private final PreferenceComponentAnnotatedClass annotatedClazz;
    private final TypeElement injectedElement;

    public InjectorGenerator(@NonNull PreferenceComponentAnnotatedClass annotatedClass, @NonNull TypeElement injectedElement) {
        this.annotatedClazz = annotatedClass;
        this.injectedElement = injectedElement;
    }

    public TypeSpec generate() {
        return TypeSpec.classBuilder(getClazzName())
                .addJavadoc("Generated by PreferenceRoom. (https://github.com/skydoves/PreferenceRoom).\n")
                .addModifiers(PUBLIC)
                .addMethod(getConstructorSpec())
                .build();
    }

    public MethodSpec getConstructorSpec() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(TypeName.get(injectedElement.asType()), INJECT_OBJECT).addAnnotation(NonNull.class).build());

        injectedElement.getEnclosedElements().stream()
                .filter(field -> field.getKind().isField())
                .forEach(field -> {
                    if(field.getAnnotation(InjectPreference.class) != null) {
                        String generatedClazzName = TypeName.get(field.asType()).toString();
                        if(annotatedClazz.generatedClazzList.contains(generatedClazzName)) {
                            builder.addStatement(INJECT_OBJECT + ".$N = " + COMPONENT_PREFIX + "$N.getInstance().$N()",
                                    field.getSimpleName(), annotatedClazz.clazzName, TypeName.get(field.asType()).toString().replace(PREFERENCE_PREFIX, ""));
                        } else {
                            throw new VerifyException(String.format("'%s' type can not be injected", generatedClazzName));
                        }
                    }
                });

        return builder.build();
    }

    private String getClazzName() {
        return CLAZZ_PREFIX + injectedElement.getSimpleName();
    }
}

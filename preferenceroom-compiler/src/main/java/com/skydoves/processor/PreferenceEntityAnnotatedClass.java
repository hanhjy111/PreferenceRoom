/*
 * Copyright (C) 2017 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.processor;

import android.support.annotation.NonNull;

import com.google.common.base.Strings;
import com.google.common.base.VerifyException;
import com.skydoves.preferenceroom.PreferenceEntity;
import com.skydoves.preferenceroom.PreferenceFunction;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public class PreferenceEntityAnnotatedClass {

    public final String packageName;
    public final TypeElement annotatedElement;
    public final TypeName typeName;
    public final String clazzName;
    public final String preferenceName;
    public final List<PreferenceKeyField> keyFields;

    public final List<String> keyNameFields;
    public final Map<String, PreferenceKeyField> keyFieldMap;
    public final Map<String, Element> setterFunctionsList;
    public final Map<String, Element> getterFunctionsList;

    private static final String SETTER_PREFIX = "put";
    private static final String GETTER_PREFIX = "get";
    private static final String HAS_PREFIX = "contains";
    private static final String REMOVE_PREFIX = "remove";

    public PreferenceEntityAnnotatedClass(@NonNull TypeElement annotatedElement, @NonNull Elements elementUtils) throws VerifyException {
        PreferenceEntity preferenceRoom = annotatedElement.getAnnotation(PreferenceEntity.class);
        PackageElement packageElement = elementUtils.getPackageOf(annotatedElement);
        this.packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        this.annotatedElement = annotatedElement;
        this.typeName = TypeName.get(annotatedElement.asType());
        this.clazzName = annotatedElement.getSimpleName().toString();
        this.preferenceName = Strings.isNullOrEmpty(preferenceRoom.name()) ? StringUtils.toUpperCamel(this.clazzName) : preferenceRoom.name();
        this.keyFields = new ArrayList<>();
        this.keyNameFields = new ArrayList<>();
        this.keyFieldMap = new HashMap<>();
        this.setterFunctionsList = new HashMap<>();
        this.getterFunctionsList = new HashMap<>();

        if(Strings.isNullOrEmpty(preferenceName)) {
            throw new VerifyException("You should specify PreferenceRoom class name.");
        }

        Map<String, String> checkMap = new HashMap<>();
        annotatedElement.getEnclosedElements().stream()
                .filter(variable -> variable instanceof VariableElement)
                .map(variable -> (VariableElement) variable)
                .forEach(variable -> {
                    try {
                        PreferenceKeyField keyField = new PreferenceKeyField(variable, elementUtils);

                        if(checkMap.get(keyField.keyName) != null) {
                            throw new VerifyException(String.format("\'%s\' key is already used in class.", keyField.keyName));
                        }

                        checkMap.put(keyField.keyName, keyField.clazzName);
                        keyFields.add(keyField);
                        keyNameFields.add(keyField.keyName);
                        keyFieldMap.put(keyField.keyName, keyField);
                    } catch (IllegalAccessException e) {
                        throw new VerifyException(e.getMessage());
                    }
                });

        checkOverrideMethods();

        annotatedElement.getEnclosedElements().stream()
                .filter(function -> !function.getKind().isField() && function.getModifiers().contains(Modifier.PUBLIC) &&
                        function.getAnnotation(PreferenceFunction.class) != null).forEach(function -> {
                            PreferenceFunction annotation = function.getAnnotation(PreferenceFunction.class);
                            MethodSpec methodSpec = MethodSpec.overriding((ExecutableElement) function).build();
                            if(methodSpec.parameters.size() > 1 || methodSpec.parameters.size() == 0) {
                                throw new VerifyException("PreferenceFunction should has one parameter");
                            } else if(!methodSpec.parameters.get(0).type.equals(keyFieldMap.get(annotation.keyname()).typeName)) {
                                throw new VerifyException(String.format("parameter '%s''s type should be %s.", methodSpec.parameters.get(0).name, keyFieldMap.get(annotation.keyname()).typeName));
                            } else if(!methodSpec.returnType.equals(keyFieldMap.get(annotation.keyname()).typeName)) {
                                throw new VerifyException(String.format("method '%s''s return type should be %s.", methodSpec.name, keyFieldMap.get(annotation.keyname()).typeName));
                            }
                            if(keyNameFields.contains(annotation.keyname())) {
                                if(function.getSimpleName().toString().startsWith(SETTER_PREFIX)) {
                                    setterFunctionsList.put(annotation.keyname(), function);
                                } else if(function.getSimpleName().toString().startsWith(GETTER_PREFIX)) {
                                    getterFunctionsList.put(annotation.keyname(), function);
                                } else {
                                    throw new VerifyException(String.format("PreferenceFunction's prefix should startWith 'get' or 'put' : %s", function.getSimpleName()));
                                }
                            } else {
                                throw new VerifyException(String.format("keyName '%s' is not exist in entity.", annotation.keyname()));
                            }
                });
    }

    private void checkOverrideMethods() {
        annotatedElement.getEnclosedElements().
                forEach(method -> {
                    if(keyNameFields.contains(method.getSimpleName().toString().replace(SETTER_PREFIX, "")))
                        throw new VerifyException(getMethodNameVerifyErrorMessage(method.getSimpleName().toString()));
                    else if(keyNameFields.contains(method.getSimpleName().toString().replace(GETTER_PREFIX, "")))
                        throw new VerifyException(getMethodNameVerifyErrorMessage(method.getSimpleName().toString()));
                    else if(keyNameFields.contains(method.getSimpleName().toString().replace(HAS_PREFIX, "")))
                        throw new VerifyException(getMethodNameVerifyErrorMessage(method.getSimpleName().toString()));
                    else if(keyNameFields.contains(method.getSimpleName().toString().replace(REMOVE_PREFIX, "")))
                        throw new VerifyException(getMethodNameVerifyErrorMessage(method.getSimpleName().toString()));
                });
    }

    private String getMethodNameVerifyErrorMessage(String methodName) {
        return String.format("can not use method name '%s'. Use an another one.", methodName);
    }
}

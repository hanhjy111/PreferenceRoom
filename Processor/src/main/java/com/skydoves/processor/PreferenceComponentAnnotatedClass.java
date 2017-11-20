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

import com.google.common.base.VerifyException;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class PreferenceComponentAnnotatedClass {

    public final String packageName;
    public final TypeElement annotatedElement;
    public final TypeName typeName;
    public final String clazzName;
    public final List<String> entities;

    public PreferenceComponentAnnotatedClass(@NonNull TypeElement annotatedElement, @NonNull Elements elementUtils) throws VerifyException {
        PackageElement packageElement = elementUtils.getPackageOf(annotatedElement);
        this.packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        this.annotatedElement = annotatedElement;
        this.typeName = TypeName.get(annotatedElement.asType());
        this.clazzName = annotatedElement.getSimpleName().toString();
        this.entities = new ArrayList<>();

        Set<String> entitySet = new HashSet<>();
        annotatedElement.getAnnotationMirrors().forEach(annotationMirror -> {
            annotationMirror.getElementValues().forEach((type, value) -> {
                String[] values = value.getValue().toString().split(",");
                List<String> valueList =  Arrays.asList(values);
                entitySet.addAll(valueList);
            });
        });

        entities.addAll(entitySet);
    }
}

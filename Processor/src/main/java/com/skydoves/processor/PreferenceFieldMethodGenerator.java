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

import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;

public class PreferenceFieldMethodGenerator {

    private final PreferenceKeyField keyField;
    private final String preference;

    private static final String SETTER_PREFIX = "put";
    private static final String GETTER_PREFIX = "get";
    private static final String HAS_PREFIX = "contains";
    private static final String REMOVE_PREFIX = "remove";

    private static final String EDIT_METHOD = "edit()";
    private static final String APPLY_METHOD = "apply()";

    public PreferenceFieldMethodGenerator(PreferenceKeyField keyField, String preference) {
        this.keyField = keyField;
        this.preference = preference;
    }

    public List<MethodSpec> getFieldMethods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        methodSpecs.add(generateGetter());
        methodSpecs.add(generateSetter());
        methodSpecs.add(generateContainsSpec());
        methodSpecs.add(generateRemoveSpec());
        return methodSpecs;
    }

    public MethodSpec generateGetter() {
        return MethodSpec.methodBuilder(getGetterPrefixName())
                .addModifiers(PUBLIC)
                .addStatement(getGetterStatement(), preference, keyField.keyName, keyField.value)
                .returns(keyField.typeName)
                .build();
    }

    public MethodSpec generateSetter() {
        return MethodSpec.methodBuilder(getSetterPrefixName())
                .addModifiers(PUBLIC)
                .addParameter(keyField.typeName, keyField.keyName.toLowerCase())
                .addStatement(getSetterStatement(), preference, EDIT_METHOD, keyField.keyName, keyField.keyName.toLowerCase(), APPLY_METHOD)
                .build();
    }

    public MethodSpec generateContainsSpec() {
        return MethodSpec.methodBuilder(getContainsPrefixName())
                .addModifiers(PUBLIC)
                .addStatement("return $N.contains($S)", preference, keyField.keyName)
                .returns(boolean.class)
                .build();
    }

    public MethodSpec generateRemoveSpec() {
        return MethodSpec.methodBuilder(getRemovePrefixName())
                .addModifiers(PUBLIC)
                .addStatement("$N.$N.remove($S).$N", preference, EDIT_METHOD, keyField.keyName, APPLY_METHOD)
                .build();
    }

    private String getGetterPrefixName() {
        return GETTER_PREFIX + this.keyField.keyName;
    }

    private String getSetterPrefixName() {
        return SETTER_PREFIX + this.keyField.keyName;
    }

    private String getContainsPrefixName() {
        return HAS_PREFIX + this.keyField.keyName;
    }

    private String getRemovePrefixName() {
        return REMOVE_PREFIX + this.keyField.keyName;
    }

    private String getGetterTypeMethodName() {
        return GETTER_PREFIX + StringUtils.toUpperCamel(this.keyField.typeStringName);
    }

    private String getSetterTypeMethodName() {
        return SETTER_PREFIX + StringUtils.toUpperCamel(this.keyField.typeStringName);
    }

    private String getGetterStatement() {
        if(keyField.value instanceof String)
            return "return $N.getString($S, $S)";
        else if(keyField.value instanceof Float)
            return "return $N." + getGetterTypeMethodName() + "($S, $Lf)";
        else
            return "return $N." + getGetterTypeMethodName() + "($S, $L)";
    }

    private String getSetterStatement() {
            return "$N.$N." + getSetterTypeMethodName() + "($S, $N).$N";
    }
}

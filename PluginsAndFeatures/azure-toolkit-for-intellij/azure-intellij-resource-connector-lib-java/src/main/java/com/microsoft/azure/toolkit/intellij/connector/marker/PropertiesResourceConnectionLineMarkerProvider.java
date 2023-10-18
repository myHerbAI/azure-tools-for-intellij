/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.marker;

import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

public class PropertiesResourceConnectionLineMarkerProvider extends AbstractResourceConnectionLineMarkerProvider {
    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof PropertyValueImpl;
    }

    @Override
    protected Connection<? extends AzResource, ?> getConnectionForPsiElement(@Nonnull PsiElement element) {
        final String value = getEnvironmentVariableFromPsiElement(element);
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final Connection<AzResource, ?> connection = AbstractResourceConnectionLineMarkerProvider.getConnectionWithEnvironmentVariable(module, value);
        if (Objects.isNull(connection)) {
            return null;
        }
        final ResourceDefinition<AzResource> definition = connection.getResource().getDefinition();
        final List<Pair<String, String>> variables = definition instanceof SpringSupported<AzResource> ?
                ((SpringSupported<AzResource>) definition).getSpringProperties() : Collections.emptyList();
        final Property parent = PsiTreeUtil.getParentOfType(element, Property.class);
        final String property = Optional.ofNullable(parent).map(Property::getKey).orElse(null);
        return CollectionUtils.isNotEmpty(variables) && StringUtils.equalsIgnoreCase(variables.get(0).getKey(), property) ? connection : null;
    }

    private String getEnvironmentVariableFromPsiElement(@Nonnull PsiElement element) {
        final String text = element.getText();
        final Matcher matcher = SPRING_PROPERTY_VALUE_PATTERN.matcher(text);
        return matcher.matches() ? matcher.group(1) : text;
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern SPRING_PROPERTY_VALUE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionForPropertiesValue(@Nonnull PropertyValueImpl element) {
        final String origin = element.getText();
        final Matcher matcher = SPRING_PROPERTY_VALUE_PATTERN.matcher(origin);
        final String text = matcher.matches() ? matcher.group(1) : origin;
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return getConnectionWithEnvironmentVariable(module, text);
    }

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionForYamlPlainText(@Nonnull YAMLPlainTextImpl element) {
        final String origin = element.getText();
        final Matcher matcher = SPRING_PROPERTY_VALUE_PATTERN.matcher(origin);
        final String text = matcher.matches() ? matcher.group(1) : origin;
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return getConnectionWithEnvironmentVariable(module, text);
    }

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionWithEnvironmentVariable(@Nullable final Module module,
                                                                                 @Nonnull String variable) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        return (Connection<? extends AzResource, ?>) defaultProfile.getConnections().stream()
                .filter(c -> isConnectionVariable(variable, defaultProfile, c))
                .findAny().orElse(null);
    }

    private static boolean isConnectionVariable(String variable, Profile defaultProfile, Connection<?, ?> c) {
        return defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable));
    }
}

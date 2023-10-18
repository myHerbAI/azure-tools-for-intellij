/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.marker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public abstract class AbstractResourceConnectionLineMarkerProvider implements LineMarkerProvider {

    public static final Pattern SPRING_PROPERTY_VALUE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    @Override
    @Nullable
    public LineMarkerInfo<?> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (shouldAccept(element)) {
            final Connection<? extends AzResource, ?> connection = getConnectionForPsiElement(element);
            final AzureServiceResource<?> resource = Optional.ofNullable(connection)
                    .filter(c -> c.getResource() instanceof AzureServiceResource)
                    .map(c -> ((AzureServiceResource<?>) c.getResource())).orElse(null);
            if (Objects.nonNull(resource)) {
                return new ResourceConnectionLineMarkerInfo(connection, resource, element);
            }
        }
        return null;
    }

    protected abstract boolean shouldAccept(@Nonnull final PsiElement element);

    @Nullable
    protected abstract Connection<? extends AzResource, ?> getConnectionForPsiElement(@Nonnull final PsiElement element);

    @Nullable
    public static Connection<AzResource, ?> getConnectionWithEnvironmentVariable(@Nullable final Module module,
                                                                                 @Nonnull String variable) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        return (Connection<AzResource, ?>) defaultProfile.getConnections().stream()
                .filter(c -> isConnectionVariable(variable, defaultProfile, c))
                .findAny().orElse(null);
    }

    private static boolean isConnectionVariable(String variable, Profile defaultProfile, Connection<?, ?> c) {
        return defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable));
    }
}

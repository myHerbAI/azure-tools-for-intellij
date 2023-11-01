/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern SPRING_PROPERTY_VALUE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    public static String extractVariableFromSpringProperties(final String origin) {
        final Matcher matcher = SPRING_PROPERTY_VALUE_PATTERN.matcher(origin);
        return matcher.matches() ? matcher.group(1) : origin;
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

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionWithResource(@Nullable final Module module,
                                                                                           @Nonnull AzResource resource) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        return (Connection<? extends AzResource, ?>) defaultProfile.getConnections().stream()
                .filter(c -> Objects.equals(c.getResource().getData(), resource))
                .findAny().orElse(null);
    }

    private static boolean isConnectionVariable(String variable, Profile defaultProfile, Connection<?, ?> c) {
        return defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable));
    }

    @Nullable
    public static PsiElement getPropertyField(final String fullQualifiedFieldName, final PsiFile file) {
        if (StringUtils.isNotBlank(fullQualifiedFieldName)) {
            final String[] parts = fullQualifiedFieldName.split("#");
            final PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(parts[0], file.getResolveScope());
            return Optional.ofNullable(psiClass).map(c -> c.findFieldByName(parts[1], true)).orElse(null);
        }
        return null;
    }


    public static List<? extends Resource<?>> listResourceForDefinition(@Nonnull final Project project,
                                                                        @Nonnull final ResourceDefinition<?> d) {
        try {
            return d.getResources(project);
        } catch (final ProcessCanceledException ProcessCanceledException) {
            throw ProcessCanceledException;
        } catch (final Exception e) {
            // swallow all exceptions except ProcessCanceledException
            return Collections.emptyList();
        }
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    private static boolean isConnectionVariable(String variable, Profile defaultProfile, Connection<?, ?> c) {
        return defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable));
    }

    public static void createAndInsert(@Nonnull Module module, @Nonnull Resource resource,
                                       @Nonnull InsertionContext context, @Nonnull ConnectionManager connectionManager,
                                       @Nonnull BiConsumer<Connection, InsertionContext> insertHandler,
                                       @Nullable Consumer<InsertionContext> cancelHandler) {
        final Project project = context.getProject();
        if (resource.canConnectSilently()) {
            final Connection<?, ?> c = createSilently(module, resource, connectionManager);
            WriteCommandAction.runWriteCommandAction(project, () -> insertHandler.accept(c, context));
        } else {
            AzureTaskManager.getInstance().runLater(() -> {
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                dialog.setResource(resource);
                if (dialog.showAndGet()) {
                    final Connection<?, ?> c = dialog.getValue();
                    WriteCommandAction.runWriteCommandAction(project, () -> insertHandler.accept(c, context));
                } else {
                    WriteCommandAction.runWriteCommandAction(project, () -> Optional.ofNullable(cancelHandler).ifPresent(c -> c.accept(context)));
                }
            });
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Connection<?, ?> createSilently(Module module, @Nonnull Resource resource,  ConnectionManager connectionManager) {
        final Resource consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
        final ConnectionDefinition<?, ?> connectionDefinition = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(), consumer.getDefinition());
        final Connection<?, ?> connection = connectionDefinition.define(resource, consumer);
        if (resource.getDefinition().isEnvPrefixSupported()) {
            connection.setEnvPrefix(connectionManager.getNewPrefix(resource, consumer));
        }
        final AzureTaskManager taskManager = AzureTaskManager.getInstance();
        taskManager.runLater(() -> taskManager.write(() -> {
            final Profile profile = connectionManager.getProfile().getModule().initializeWithDefaultProfileIfNot();
            profile.createOrUpdateConnection(connection);
            profile.save();
        }));
        return connection;
    }

    @Nullable
    public static PsiElement getPropertyField(final String fullQualifiedFieldName, final PsiFile file) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(fullQualifiedFieldName)) {
            final String[] parts = fullQualifiedFieldName.split("#");
            final PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(parts[0], file.getResolveScope());
            return Optional.ofNullable(psiClass).map(c -> c.findFieldByName(parts[1], true)).orElse(null);
        }
        return null;
    }
}

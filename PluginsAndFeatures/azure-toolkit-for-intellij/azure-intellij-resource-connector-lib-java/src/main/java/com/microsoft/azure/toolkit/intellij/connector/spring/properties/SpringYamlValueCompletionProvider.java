/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringYamlValueCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final YAMLPsiElement yamlElement = PsiTreeUtil.getParentOfType(element, YAMLPsiElement.class);
        if (Objects.isNull(yamlElement)) {
            return;
        }
        final String currentKey = YAMLUtil.getConfigFullName(yamlElement);
        final List<ResourceDefinition<?>> definitions = ResourceManager.getDefinitions();
        addYamlValueLookupElements(currentKey, definitions, parameters, result);
    }

    private void addYamlValueLookupElements(@Nonnull final String key, @Nonnull final List<ResourceDefinition<?>> definitions,
                                            @Nonnull CompletionParameters parameters, @Nonnull final CompletionResultSet result) {
        // find correspond resource definition
        final SpringSupported<?> springSupported = definitions.stream()
                .filter(d -> d instanceof SpringSupported<?>)
                .map(d -> (SpringSupported<?>) d)
                .filter(d -> d.getSpringProperties().stream().anyMatch(p -> StringUtils.equals(p.getKey(), key)))
                .findFirst().orElse(null);
        final Module module = ModuleUtil.findModuleForPsiElement(parameters.getOriginalFile());
        if (Objects.isNull(module) || Objects.isNull(springSupported) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final List<? extends Resource<?>> resources = springSupported.getResources(module.getProject());
        final Pair<String, String> property = springSupported.getSpringProperties().stream()
                .filter(p -> StringUtils.equals(p.getKey(), key))
                .findFirst().orElseThrow(() -> new RuntimeException("cannot find property with key " + key));
        resources.stream()
                .map(resource -> createYamlValueLookupElement(module, resource, springSupported, property.getValue()))
                .filter(Objects::nonNull)
                .forEach(result::addElement);
    }

    @Nullable
    private LookupElement createYamlValueLookupElement(@Nonnull final Module module, @Nonnull final Resource<?> azResource,
                                                       @Nonnull final SpringSupported<?> definition, @Nonnull final String template) {
        final AzureModule azureModule = AzureModule.from(module);
        final Profile defaultProfile = azureModule.getDefaultProfile();
        final List<Connection<?, ?>> connections = Objects.isNull(defaultProfile) ? Collections.emptyList() : defaultProfile.getConnections();
        final Connection<?, ?> connection = connections.stream()
                .filter(c -> Objects.equals(c.getResource(), azResource)).findFirst()
                .orElseGet(() -> createConnection(module, azResource));
        final String elementValue = YamlUtils.getPropertiesCompletionValue(template, connection);
        return LookupElementBuilder.create(elementValue)
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withPresentableText(azResource.getName())
                .withTypeText("String")
                .withLookupStrings(List.of(elementValue, azResource.getName()))
                .withInsertHandler((context, item) -> {
                    saveConnection(azureModule, connection);
                    YamlUtils.insertYamlConnection(connection, context);
                });
    }

    public static Connection<?, ?> createConnection(@Nonnull Module module, @Nonnull Resource<?> resource) {
        final Resource<?> consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
        // todo: set connection env prefix
        final ConnectionDefinition connectionDefinition =
                ConnectionManager.getDefinitionOrDefault(resource.getDefinition(), consumer.getDefinition());
        final Connection<?, ?> result = connectionDefinition.define(resource, consumer);
        result.setEnvPrefix(StringUtils.upperCase(resource.getName()));
        return result;
    }

    private static void saveConnection(@Nonnull final AzureModule module, @Nonnull final Connection<?, ?> connection) {
        final AzureTaskManager taskManager = AzureTaskManager.getInstance();
        taskManager.runOnPooledThread(() ->
                taskManager.runLater(() ->
                        taskManager.write(() -> {
                            final Profile profile = Optional.ofNullable(module.getDefaultProfile())
                                    .orElseGet(module::initializeWithDefaultProfileIfNot);
                            profile.createOrUpdateConnection(connection);
                            profile.save();
                        })));
    }
}


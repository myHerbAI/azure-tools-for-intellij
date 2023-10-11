/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PropertyValueCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module)) {
            return;
        }
        final String key = parameters.getPosition().getParent().getFirstChild().getText();
        final List<? extends SpringSupported<?>> definitions = getSupportedDefinitions(key);
        definitions.stream().flatMap(d -> d.getResources(module.getProject()).stream())
            .map(r -> LookupElementBuilder
                .create(r.getName(), r.getName())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(r.getDefinition().getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withLookupStrings(Arrays.asList(r.getName(), ((AzResource) r.getData()).getResourceGroupName()))
                .withTypeText(((AzResource) r.getData()).getResourceGroupName()))
            .forEach(result::addElement);
    }

    private static List<? extends SpringSupported<?>> getSupportedDefinitions(String key) {
        final List<ResourceDefinition<?>> definitions = ResourceManager.getDefinitions(ResourceDefinition.RESOURCE).stream().filter(d -> d instanceof SpringSupported<?>).toList();
        return definitions.stream().map(d -> (SpringSupported<?>) d)
            .filter(d -> d.getSpringProperties().stream().anyMatch(p -> p.getKey().equals(key)))
            .toList();
    }
}

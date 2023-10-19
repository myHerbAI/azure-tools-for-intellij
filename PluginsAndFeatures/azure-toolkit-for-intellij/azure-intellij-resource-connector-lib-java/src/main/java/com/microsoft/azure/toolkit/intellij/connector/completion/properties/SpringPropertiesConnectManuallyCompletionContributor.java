/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion.properties;

import com.intellij.codeInsight.completion.*;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.completion.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class SpringPropertiesConnectManuallyCompletionContributor extends CompletionContributor {
    public SpringPropertiesConnectManuallyCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.VALUE_CHARACTERS), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@Nonnull final CompletionParameters parameters, @Nonnull final ProcessingContext context, @Nonnull final CompletionResultSet result) {
                final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                if (Objects.isNull(module)) {
                    return;
                }
                final String key = parameters.getPosition().getParent().getFirstChild().getText();
                final List<? extends SpringSupported<?>> definitions = SpringPropertyValueCompletionProvider.getSupportedDefinitions(key);
                if (!definitions.isEmpty()) {
                    if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                        result.addElement(LookupElements.buildSignInLookupElement());
                    } else {
                        result.addElement(LookupElements.buildConnectLookupElement(definitions.get(0), SpringPropertyValueCompletionProvider.PropertyValueInsertHandler::insert));
                    }
                }
            }
        });
    }
}

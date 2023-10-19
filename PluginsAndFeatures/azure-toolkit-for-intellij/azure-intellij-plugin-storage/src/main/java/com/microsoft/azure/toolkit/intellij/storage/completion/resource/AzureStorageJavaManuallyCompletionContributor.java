/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.completion.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class AzureStorageJavaManuallyCompletionContributor extends CompletionContributor {

    public AzureStorageJavaManuallyCompletionContributor() {
        super();
        extend(null, psiElement().inside(literalExpression()), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull final CompletionParameters parameters, @NotNull final ProcessingContext context, @NotNull final CompletionResultSet result) {
                final PsiElement element = parameters.getPosition();
                final String fullPrefix = element.getText().split(AzureStorageJavaCompletionContributor.DUMMY_IDENTIFIER)[0].replace("\"", "").trim();
                final boolean isBlobContainer = fullPrefix.startsWith("azure-blob://");
                final boolean isFileShare = fullPrefix.startsWith("azure-file://");

                if (isBlobContainer || isFileShare) {
                    final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                    if (Objects.isNull(module)) {
                        return;
                    }
                    if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                        result.addElement(LookupElements.buildSignInLookupElement());
                    } else if (!hasConnections(module)) {
                        result.addElement(LookupElements.buildConnectLookupElement(StorageAccountResourceDefinition.INSTANCE, (definition, ctx) -> {
                            if (Objects.nonNull(definition)) {
                                AutoPopupController.getInstance(ctx.getProject()).scheduleAutoPopup(ctx.getEditor());
                            }
                        }));
                    }
                    result.stopHere();
                }
            }
        });
    }

    private static boolean hasConnections(Module module) {
        return Optional.of(module).map(AzureModule::from)
            .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
            .flatMap(m -> m.getConnections().stream())
            .anyMatch(c -> c.getDefinition().getResourceDefinition() instanceof StorageAccountResourceDefinition);
    }
}

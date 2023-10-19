/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.AzureStorageJavaCompletionContributor.DUMMY_IDENTIFIER;

public class AzureStoragePrefixStringLiteralCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final String[] parts = element.getText().split(DUMMY_IDENTIFIER);
        final String fullPrefix = parts.length > 0 ? parts[0].replace("\"", "").trim() : element.getText();
        final boolean isBlobContainer = fullPrefix.startsWith("azure-blob://");
        final boolean isFileShare = fullPrefix.startsWith("azure-file://");

        if (!isBlobContainer && !isFileShare) {
            result.addElement(LookupElementBuilder.create("azure-blob://")
                .withInsertHandler(new MyInsertHandler())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.CONTAINERS)));
            result.addElement(LookupElementBuilder.create("azure-file://")
                .withInsertHandler(new MyInsertHandler())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.SHARES)));
            result.stopHere();
        }
    }

    private static class MyInsertHandler implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
    }
}

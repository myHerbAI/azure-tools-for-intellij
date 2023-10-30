/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionStringLiteralResourceReference;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.storage.code.Utils;
import com.microsoft.azure.toolkit.intellij.storage.code.spring.StringLiteralResourceReferenceContributor;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.queue.Queue;
import com.microsoft.azure.toolkit.lib.storage.table.Table;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.storage.code.function.FunctionBlobPathCompletionProvider.BLOB_PATH_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.code.function.FunctionQueueNameCompletionProvider.QUEUE_NAME_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.code.function.FunctionTableNameCompletionProvider.TABLE_NAME_PAIR_PATTERN;

public class FunctionStorageAccountResourceReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        // blob
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(BLOB_PATH_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final StorageAccount storageAccount = Utils.getBindingStorageAccount(annotation);
                if (Objects.nonNull(storageAccount)) {
                    return StringLiteralResourceReferenceContributor.getStorageBlobReferences(StringUtils.EMPTY, "azure-blob://", literal, storageAccount);
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
        // queue
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(QUEUE_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
                final StorageAccount storageAccount = Utils.getBindingStorageAccount(annotation);
                final Queue queue = Optional.ofNullable(storageAccount)
                        .map(account -> account.getQueueModule().get(value, account.getResourceGroupName()))
                        .orElse(null);
                if (Objects.nonNull(queue)) {
                    final TextRange range = new TextRange(value.indexOf(queue.getName()) + 1, value.indexOf(queue.getName()) + 1 + queue.getName().length());
                    return new PsiReference[]{new FunctionStringLiteralResourceReference(element, range, queue, Objects.requireNonNull(connection), true)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
        // table
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(TABLE_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
                final StorageAccount storageAccount = Utils.getBindingStorageAccount(annotation);
                final Table table = Optional.ofNullable(storageAccount)
                        .map(account -> account.getTableModule().get(value, account.getResourceGroupName()))
                        .orElse(null);
                if (Objects.nonNull(table)) {
                    final TextRange range = new TextRange(value.indexOf(table.getName()) + 1, value.indexOf(table.getName()) + 1 + table.getName().length());
                    return new PsiReference[]{new FunctionStringLiteralResourceReference(element, range, table, Objects.requireNonNull(connection), true)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}

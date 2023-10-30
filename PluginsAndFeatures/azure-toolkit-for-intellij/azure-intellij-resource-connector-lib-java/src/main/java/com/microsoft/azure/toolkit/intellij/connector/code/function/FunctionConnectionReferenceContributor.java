/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionProvider.CONNECTION_NAME_VALUE;

public class FunctionConnectionReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        // blob
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(CONNECTION_NAME_VALUE), new PsiReferenceProvider() {
            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
                if (Objects.nonNull(connection)) {
                    final String envPrefix = connection.getEnvPrefix();
                    final TextRange range = new TextRange(value.indexOf(envPrefix) + 1, value.indexOf(envPrefix) + 1 + envPrefix.length());
                    return new PsiReference[]{new FunctionStringLiteralResourceReference(element, range, Objects.requireNonNull(connection), true)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}

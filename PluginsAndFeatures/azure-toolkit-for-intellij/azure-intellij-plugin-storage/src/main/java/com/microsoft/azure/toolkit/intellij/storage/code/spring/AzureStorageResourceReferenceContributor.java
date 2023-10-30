/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class AzureStorageResourceReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement().inside(literalExpression()), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String text = element.getText();
                final String valueWithPrefix = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                if ((valueWithPrefix != null && (valueWithPrefix.startsWith("azure-blob://") || valueWithPrefix.startsWith("azure-file://")))) {
                    final String prefix = valueWithPrefix.startsWith("azure-blob://") ? "azure-blob://" : "azure-file://";
                    final String[] parts = new TextRange(prefix.length() + 1, valueWithPrefix.length() + 1).substring(text).split("/", -1);
                    final List<AzureStorageResourceReference> references = new ArrayList<>();
                    int startOffset = prefix.length() + 1;
                    for (final String part : parts) {
                        if (StringUtils.isBlank(part)) {
                            break;
                        }
                        final int endOffset = startOffset + part.length();
                        final TextRange range = new TextRange(startOffset, endOffset);
                        references.add(new AzureStorageResourceReference(element, range, new TextRange(1, endOffset).substring(text)));
                        startOffset = endOffset + 1;
                    }
                    return references.toArray(new PsiReference[0]);
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}
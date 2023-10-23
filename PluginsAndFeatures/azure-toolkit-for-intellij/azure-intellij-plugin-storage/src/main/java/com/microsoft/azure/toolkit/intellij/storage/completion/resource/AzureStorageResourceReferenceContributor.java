package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;

import javax.annotation.Nonnull;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class AzureStorageResourceReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement().inside(literalExpression()), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                if ((value != null && (value.startsWith("azure-blob://") || value.startsWith("azure-file://")))) {
                    final int offset = value.startsWith("azure-blob://") ? "azure-blob://".length() : "azure-file://".length();
                    final TextRange range = new TextRange(offset + 1, value.length() + 1);
                    return new PsiReference[]{new AzureStorageResourceReference(element, range)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}
/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;

import java.util.Optional;

public class AzureStorageStringLiteralCharFilter extends CharFilter {
    public static final String ANNOTATION_VALUE = "org.springframework.beans.factory.annotation.Value";

    @Override
    public Result acceptChar(char c, final int prefixLength, final Lookup lookup) {
        final PsiFile file = lookup.getPsiFile();
        if (file == null || file.getFileType() != JavaFileType.INSTANCE || !isWithinLiteral(lookup)) {
            return null;
        }
        final PsiElement element = lookup.getPsiElement();
        final String text = Optional.ofNullable(element).map(PsiElement::getText).map(t -> t.replace("\"", "")).orElse("");
        if ((c == ':' || c == '-' || c == '/') && text.startsWith("azure")
            && PsiJavaPatterns.psiElement().insideAnnotationParam(ANNOTATION_VALUE).accepts(element)) {
            return Result.ADD_TO_PREFIX;
        }
        return null;
    }

    private static boolean isWithinLiteral(final Lookup lookup) {
        final PsiElement psiElement = lookup.getPsiElement();
        return psiElement != null && psiElement.getParent() instanceof PsiLiteralExpression;
    }
}

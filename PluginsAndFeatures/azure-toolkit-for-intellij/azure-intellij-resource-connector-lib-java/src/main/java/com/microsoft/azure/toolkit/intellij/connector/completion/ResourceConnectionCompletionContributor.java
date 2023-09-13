/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.microsoft.azure.toolkit.intellij.connector.completion.provider.AnnotationCompletionProvider;
import com.microsoft.azure.toolkit.intellij.connector.completion.provider.MethodCompletionProvider;
import com.microsoft.azure.toolkit.intellij.connector.completion.provider.ConnectionStringCompletionProvider;

public class ResourceConnectionCompletionContributor extends CompletionContributor {
    public ResourceConnectionCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new MethodCompletionProvider());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new AnnotationCompletionProvider());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new ConnectionStringCompletionProvider());
    }

}

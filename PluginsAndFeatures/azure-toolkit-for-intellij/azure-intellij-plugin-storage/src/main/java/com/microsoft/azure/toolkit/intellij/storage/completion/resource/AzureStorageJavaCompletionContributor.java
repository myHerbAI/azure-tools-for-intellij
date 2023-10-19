/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;

import javax.annotation.Nonnull;

import static com.intellij.patterns.PsiJavaPatterns.*;

public class AzureStorageJavaCompletionContributor extends CompletionContributor {

    public static final String ANNOTATION_VALUE = "org.springframework.beans.factory.annotation.Value";
    public static final String DUMMY_IDENTIFIER = "AzureRulezzz";

    private static final PsiJavaElementPattern.Capture<PsiElement> insideAnnotation = psiElement().insideAnnotationParam(ANNOTATION_VALUE);
    private static final PsiMethodPattern resourceLoaderGetResource = psiMethod().withName("getResource").definedInClass("org.springframework.core.io.ResourceLoader");
    private static final PsiMethodPattern azureStorageBlobProtocolResolverGetResource = psiMethod().withName("getResource").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver ");
    private static final PsiMethodPattern azureStorageBlobProtocolResolverGetResources = psiMethod().withName("getResources").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver ");
    private static final PsiMethodPattern azureStorageFileProtocolResolverGetResource = psiMethod().withName("getResource").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageFileProtocolResolver");
    private static final PsiMethodPattern azureStorageFileProtocolResolverGetResources = psiMethod().withName("getResources").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageFileProtocolResolver");

    public static final ElementPattern<? extends PsiElement> PREFIX_SCOPES = PlatformPatterns.or(
        insideAnnotation,
        psiElement(JavaTokenType.STRING_LITERAL).withParent(psiLiteral().methodCallParameter(0, PlatformPatterns.or(
            resourceLoaderGetResource,
            azureStorageBlobProtocolResolverGetResource,
            azureStorageFileProtocolResolverGetResource,
            azureStorageBlobProtocolResolverGetResources,
            azureStorageFileProtocolResolverGetResources
        ))));

    public AzureStorageJavaCompletionContributor() {
        super();
        extend(null, PREFIX_SCOPES, new AzureStoragePrefixStringLiteralCompletionProvider());
        extend(null, psiElement().inside(literalExpression()), new AzureStorageResourceStringLiteralCompletionProvider());
    }

    @Override
    public void beforeCompletion(@Nonnull final CompletionInitializationContext context) {
        super.beforeCompletion(context);
        context.setDummyIdentifier(DUMMY_IDENTIFIER);
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults.code.spring;

import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.keyvaults.connection.KeyVaultResourceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class EnvVarReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.or(psiElement(PropertyValueImpl.class), psiElement(YAMLPlainTextImpl.class)), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                return getEnvVarReferences(element).toArray(new PsiReference[0]);
            }
        }, PsiReferenceRegistrar.HIGHER_PRIORITY);
    }

    @Nonnull
    static List<EnvVarReference> getEnvVarReferences(final @Nonnull PsiElement element) {
        final String text = element.getText();
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final boolean hasVaults = Optional.ofNullable(ModuleUtil.findModuleForPsiElement(element)).map(AzureModule::from).stream()
            .flatMap(m -> m.getConnections(KeyVaultResourceDefinition.INSTANCE).stream()).findAny().isPresent();
        if (EnvVarCompletionContributor.hasEnvVars(text) && hasVaults) {
            return getEnvVarRanges(element.getText()).stream()
                .map(r -> new EnvVarReference(element, r))
                .toList();
        }
        return Collections.emptyList();
    }

    @Nonnull
    private static List<TextRange> getEnvVarRanges(String text) {
        final List<TextRange> ranges = new ArrayList<>();
        int startPos = 0;
        while (true) {
            final TextRange range = getEnvVarRange(text, startPos);
            if (Objects.isNull(range)) {
                break;
            }
            ranges.add(range);
            startPos = range.getEndOffset() + 1;
        }
        return ranges;
    }

    @Nullable
    private static TextRange getEnvVarRange(String text, int startPos) {
        if (startPos == -1) {
            return null;
        }
        final int start = StringUtils.indexOf(text, "${", startPos);
        final int end = StringUtils.indexOf(text, "}", start + 3);
        if (start > -1 && end > -1) {
            return new TextRange(start + 2, end);
        }
        return null;
    }
}
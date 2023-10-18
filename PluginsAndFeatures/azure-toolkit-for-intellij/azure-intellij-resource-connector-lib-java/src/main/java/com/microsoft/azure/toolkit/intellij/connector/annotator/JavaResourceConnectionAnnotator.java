/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.annotator;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Utils;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class JavaResourceConnectionAnnotator extends AbstractResourceConnectionAnnotator {
    @Override
    protected String extractVariableValue(@Nonnull PsiElement element) {
        return StringUtils.strip(element.getText(), "\"");
    }

    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof PsiJavaToken && Objects.equals(((PsiJavaToken) element).getTokenType(), JavaTokenType.STRING_LITERAL);
    }

    @Nullable
    @Override
    protected Connection<? extends AzResource, ?> getConnectionForPsiElement(@Nonnull PsiElement element) {
        final String value = StringUtils.strip(element.getText(), "\"");
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return Utils.getConnectionWithEnvironmentVariable(module, value);
    }
}

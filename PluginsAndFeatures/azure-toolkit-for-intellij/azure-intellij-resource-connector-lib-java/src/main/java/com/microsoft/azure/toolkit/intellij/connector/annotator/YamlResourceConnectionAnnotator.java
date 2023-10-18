/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.annotator;

import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Utils;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;

public class YamlResourceConnectionAnnotator extends AbstractResourceConnectionAnnotator{
    @Override
    protected String extractVariableValue(@Nonnull PsiElement element) {
        return Utils.extractVariableFromSpringProperties(element.getText());
    }

    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof YAMLPlainTextImpl;
    }

    @Override
    protected Connection<? extends AzResource, ?> getConnectionForPsiElement(@Nonnull PsiElement element) {
        return Utils.getConnectionForYamlPlainText((YAMLPlainTextImpl) element);
    }
}

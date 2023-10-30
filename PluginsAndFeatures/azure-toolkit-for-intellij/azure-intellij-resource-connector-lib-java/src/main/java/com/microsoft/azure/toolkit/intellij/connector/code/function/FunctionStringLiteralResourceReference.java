/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;

public class FunctionStringLiteralResourceReference extends PsiReferenceBase<PsiElement> {
    private final AbstractAzResource<?, ?, ?> resource;
    private final Connection<?, ?> connection;

    public FunctionStringLiteralResourceReference(@Nonnull final PsiElement element, @Nonnull TextRange rangeInElement,
                                                  @Nonnull final AbstractAzResource<?, ?, ?> resource,
                                                  @Nonnull final Connection<?, ?> connection,
                                                  boolean soft) {
        super(element, rangeInElement, soft);
        this.resource = resource;
        this.connection = connection;
    }

    @Override
    public @Nullable PsiElement resolve() {
        final Module module = ModuleUtil.findModuleForFile(this.getElement().getContainingFile());
        if (Objects.isNull(module)) {
            return null;
        }
        return new AzureCosmosResourcePsiElement(resource, connection);
    }


    @AllArgsConstructor
    class AzureCosmosResourcePsiElement extends FakePsiElement implements SyntheticElement {
        private final AbstractAzResource<?, ?, ?> resource;
        private final Connection<?, ?> connection;

        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        @AzureOperation("user/connector.navigate_to_resource_from_function_string_literal")
        public void navigate(boolean requestFocus) {
            final Module module = ModuleUtil.findModuleForPsiElement(getElement());
            AbstractAzureFacetNode.selectConnectedResource(connection, resource.getId(), true);
        }

        @Override
        public String getPresentableText() {
            return resource.getName();
        }

        @Override
        public String getName() {
            return resource.getName();
        }

        @Override
        public @Nullable String getLocationString() {
            return resource.getId();
        }

        @Override
        public @Nullable Icon getIcon(final boolean open) {
            return IntelliJAzureIcons.getIcon(AzureResourceIconProvider.getResourceIconPath(resource));
        }

        @Override
        public TextRange getTextRange() {
            final TextRange rangeInElement = getRangeInElement();
            final TextRange elementRange = myElement.getTextRange();
            return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
        }
    }
}

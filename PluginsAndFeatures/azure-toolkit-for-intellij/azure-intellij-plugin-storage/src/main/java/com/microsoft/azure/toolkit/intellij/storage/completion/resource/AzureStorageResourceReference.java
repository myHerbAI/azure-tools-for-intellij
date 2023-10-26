/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class AzureStorageResourceReference extends PsiReferenceBase<PsiElement> {
    private String fullNameWithPrefix;

    public AzureStorageResourceReference(@NotNull PsiElement element, TextRange rangeInElement, boolean soft) {
        super(element, rangeInElement, soft);
    }

    public AzureStorageResourceReference(@NotNull PsiElement element, TextRange rangeInElement, final @NotNull String fullNameWithPrefix) {
        super(element, rangeInElement);
        this.fullNameWithPrefix = fullNameWithPrefix;
    }

    public AzureStorageResourceReference(@NotNull PsiElement element, boolean soft) {
        super(element, soft);
    }

    public AzureStorageResourceReference(@NotNull PsiElement element) {
        super(element);
    }

    @Override
    public @Nullable PsiElement resolve() {
        final Module module = ModuleUtil.findModuleForFile(this.getElement().getContainingFile());
        if (Objects.isNull(module)) {
            return null;
        }
        final StorageFile file = AzureStorageResourceStringLiteralCompletionProvider.getFile(fullNameWithPrefix, module);
        if (Objects.nonNull(file)) {
            return new AzureStorageResourcePsiElement(file);
        }
        return null;
    }

    class AzureStorageResourcePsiElement extends FakePsiElement implements SyntheticElement {
        private final StorageFile file;

        public AzureStorageResourcePsiElement(final StorageFile file) {
            super();
            this.file = file;
        }

        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        @AzureOperation("user/connector.navigate_to_storage_resource_from_string_literal")
        public void navigate(boolean requestFocus) {
            final Module module = ModuleUtil.findModuleForPsiElement(getElement());
            AzureStorageResourceStringLiteralCompletionProvider.navigateToFile(this.file, module);
        }

        @Override
        public String getPresentableText() {
            return file.getName();
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public @Nullable String getLocationString() {
            final StorageAccount account = AzureStorageResourceStringLiteralCompletionProvider.getStorageAccount(this.file);
            if (Objects.nonNull(account)) {
                return this.file.getResourceGroupName() + "/" + account.getName();
            } else {
                return this.file.getResourceGroupName();
            }
        }

        @Override
        public @Nullable Icon getIcon(final boolean open) {
            return IntelliJAzureIcons.getIcon(AzureStorageResourceStringLiteralCompletionProvider.getFileIcon(this.file));
        }

        @Override
        public TextRange getTextRange() {
            final TextRange rangeInElement = getRangeInElement();
            final TextRange elementRange = myElement.getTextRange();
            return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
        }
    }
}

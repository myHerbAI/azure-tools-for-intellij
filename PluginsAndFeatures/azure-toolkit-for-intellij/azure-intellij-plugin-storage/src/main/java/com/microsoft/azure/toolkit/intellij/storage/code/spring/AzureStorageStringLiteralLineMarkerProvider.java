/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class AzureStorageStringLiteralLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    public LineMarkerInfo<?> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (psiElement().inside(literalExpression()).accepts(element) && element.getParent() instanceof PsiLiteralExpression literal) {
            final Module module = ModuleUtil.findModuleForPsiElement(element);
            final String valueWithPrefix = literal.getValue() instanceof String ? (String) literal.getValue() : null;
            if (Objects.nonNull(module) && valueWithPrefix != null && (valueWithPrefix.startsWith("azure-blob://") || valueWithPrefix.startsWith("azure-file://"))) {
                final String prefix = valueWithPrefix.startsWith("azure-blob://") ? "azure-blob://" : "azure-file://";
                final StorageFile file = AzureStorageResourceStringLiteralCompletionProvider.getFile(valueWithPrefix, module);
                if (Objects.nonNull(file)) {
                    return new ResourceLineMarkerInfo(element, file);
                }
            }
        }
        return null;
    }

    static class ResourceLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        @Getter
        private final StorageFile file;

        public ResourceLineMarkerInfo(final PsiElement element, final StorageFile file) {
            super(element, element.getTextRange(),
                IntelliJAzureIcons.getIcon(AzureStorageResourceStringLiteralCompletionProvider.getFileIcon(file)),
                ignore -> String.format("navigate to Azure Storage %s \"%s\" in Project Explorer", file.getName(), file.getResourceTypeName()),
                null, (e, element1) -> {
                    final Module module = ModuleUtil.findModuleForPsiElement(element1);
                    AzureStorageResourceStringLiteralCompletionProvider.navigateToFile(file, module);
                }, GutterIconRenderer.Alignment.LEFT, file::getName);
            this.file = file;
        }

        @Override
        public boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info) {
            return info instanceof ResourceLineMarkerInfo &&
                Objects.equals(((ResourceLineMarkerInfo) info).file, this.file);
        }

        @Override
        public Icon getCommonIcon(@Nonnull List<? extends MergeableLineMarkerInfo<?>> infos) {
            return infos.stream()
                .filter(i -> i instanceof ResourceLineMarkerInfo)
                .map(i -> (ResourceLineMarkerInfo) i)
                .map(ResourceLineMarkerInfo::getIcon)
                .findFirst().orElse(null);
        }
    }
}

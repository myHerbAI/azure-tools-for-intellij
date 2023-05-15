/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package org.wso2.lsp4intellij.actions;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.bicep.BicepFileType;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.Objects;
import java.util.Optional;

/**
 * Action overriding QuickDoc (CTRL+Q)
 */
class LSPGotoDeclarationAction extends GotoDeclarationAction implements DumbAware {
    private final Logger LOG = Logger.getInstance(LSPGotoDeclarationAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final VirtualFile file = Optional.ofNullable(editor)
            .map(ed -> FileDocumentManager.getInstance().getFile(ed.getDocument())).orElse(null);
        if (Objects.nonNull(file) && file.getFileType() instanceof BicepFileType) {
            final EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
            if (manager != null) {
                manager.gotoDeclaration(editor);
            } else {
                super.actionPerformed(e);
            }
        } else
            super.actionPerformed(e);
    }
}

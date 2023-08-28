/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.ui.libraries.ApplicationInsightsPanel;
import com.microsoft.intellij.util.MavenUtils;

import javax.annotation.Nonnull;

public class ApplicationInsightsAction extends AnAction {
    @Override
    public void actionPerformed(@Nonnull AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        DefaultDialogWrapper dialog = new DefaultDialogWrapper(module.getProject(), new ApplicationInsightsPanel(module));
        dialog.show();
    }

    @Override
    @ExceptionNotification
    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        boolean isMavenOrNull = (module == null || MavenUtils.isMavenProject(module.getProject()));
        event.getPresentation().setEnabledAndVisible(!isMavenOrNull && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}

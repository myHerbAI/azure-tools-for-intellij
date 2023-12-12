/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples.view;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class AzureSamplesCloneDialogExtensionComponent extends VcsCloneDialogExtensionComponent {
    private final Project project;

    public AzureSamplesCloneDialogExtensionComponent(final Project project, final ModalityState modalityState) {
        super();
        this.project = project;
    }

    @Override
    public void doClone(@Nonnull final CheckoutProvider.Listener listener) {
        System.out.println("doClone");
    }

    @Nonnull
    @Override
    public List<ValidationInfo> doValidateAll() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public JComponent getView() {
        return new GithubRepositoriesView(project).getContentPanel();
    }

    @Override
    public void onComponentSelected() {
        getDialogStateListener().onOkActionEnabled(true);
        System.out.println("onComponentSelected");
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return super.getPreferredFocusedComponent();
    }
}

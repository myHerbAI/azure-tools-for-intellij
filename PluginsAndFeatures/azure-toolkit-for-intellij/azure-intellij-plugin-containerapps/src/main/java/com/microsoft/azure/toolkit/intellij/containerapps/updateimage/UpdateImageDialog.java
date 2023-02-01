/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.updateimage;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;

public class UpdateImageDialog extends AzureDialog<UpdateImageForm.UpdateImageConfig> {
    private JPanel contentPanel;
    @Getter
    private UpdateImageForm form;

    public UpdateImageDialog(@Nullable Project project) {
        super(project);
        this.init();
        this.pack();
    }

    @Override
    protected String getDialogTitle() {
        return "Update Image";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }
}

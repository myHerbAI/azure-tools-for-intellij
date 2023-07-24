/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterDraft;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;

public class SpringCloudClusterCreationDialog extends AzureDialog<SpringCloudClusterDraft> {
    private JPanel contentPanel;
    @Getter
    private SpringCloudClusterCreationPanel form;

    public SpringCloudClusterCreationDialog(@Nullable Project project) {
        super(project);
        this.init();
        this.pack();
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Spring Apps";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }
}

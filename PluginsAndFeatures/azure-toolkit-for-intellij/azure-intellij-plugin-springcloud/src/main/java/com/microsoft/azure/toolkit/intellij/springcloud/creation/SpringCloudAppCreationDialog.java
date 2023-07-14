/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import javax.annotation.Nullable;
import javax.swing.*;

public class SpringCloudAppCreationDialog extends ConfigDialog<SpringCloudAppDraft> {
    private JPanel panel;
    private SpringCloudAppInfoBasicPanel basicForm;
    private SpringCloudAppInfoAdvancedPanel advancedForm;

    public SpringCloudAppCreationDialog() {
        this(null);
    }

    public SpringCloudAppCreationDialog(@Nullable Project project) {
        super(project);
        this.init();
        setFrontPanel(basicForm);
    }

    @Override
    protected AzureFormPanel<SpringCloudAppDraft> getAdvancedFormPanel() {
        return advancedForm;
    }

    @Override
    protected AzureFormPanel<SpringCloudAppDraft> getBasicFormPanel() {
        return basicForm;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Spring App";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    private void createUIComponents() {
        advancedForm = new SpringCloudAppInfoAdvancedPanel();
        basicForm = new SpringCloudAppInfoBasicPanel();
    }

    public void setCluster(@Nullable SpringCloudCluster cluster, Boolean fixed) {
        this.advancedForm.setCluster(cluster, fixed);
        this.basicForm.setCluster(cluster, fixed);
    }

    public void setDefaultRuntimeVersion(@Nullable Integer jdkVersion) {
        if (jdkVersion != null) {
            final RuntimeVersion runtime = jdkVersion <= 11 ? RuntimeVersion.JAVA_11 : RuntimeVersion.JAVA_17;
            this.advancedForm.setDefaultRuntimeVersion(runtime.toString());
            this.basicForm.setDefaultRuntimeVersion(runtime.toString());
        }
    }
}

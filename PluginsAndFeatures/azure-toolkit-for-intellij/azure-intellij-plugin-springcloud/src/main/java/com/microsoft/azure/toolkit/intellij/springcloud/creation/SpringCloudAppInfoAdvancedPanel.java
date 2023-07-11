/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppConfigPanel;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
public class SpringCloudAppInfoAdvancedPanel extends SpringCloudAppInfoPanel {
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private AzureTextInput textName;
    private SpringCloudAppConfigPanel formConfig;
    private JLabel lblSubscription;

    public SpringCloudAppInfoAdvancedPanel(@Nullable final SpringCloudCluster cluster) {
        super(cluster);
        $$$setupUI$$$();
        this.init();
        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
    }

    protected void onAppChanged(SpringCloudApp app) {
        AzureTaskManager.getInstance().runLater(() -> this.formConfig.updateForm(app), AzureTask.Modality.ANY);
        super.onAppChanged(app);
    }

    @Override
    public SpringCloudAppDraft getValue() {
        final SpringCloudAppDraft app = super.getValue();
        this.formConfig.applyTo(app);
        return app;
    }

    @Override
    public void setValue(final SpringCloudAppDraft app) {
        super.setValue(app);
        this.formConfig.setValue(app);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final List<AzureFormInput<?>> inputs = this.formConfig.getInputs();
        inputs.addAll(Arrays.asList(
            this.getTextName(),
            this.getSelectorSubscription(),
            this.getSelectorCluster()
        ));
        return inputs;
    }

    public void setDefaultRuntimeVersion(final String runtime) {
        this.formConfig.setDefaultRuntimeVersion(runtime);
    }
}

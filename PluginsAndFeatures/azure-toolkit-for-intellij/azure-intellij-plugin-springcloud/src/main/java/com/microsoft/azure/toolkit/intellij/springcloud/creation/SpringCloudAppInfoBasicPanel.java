/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.intellij.icons.AllIcons;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
public class SpringCloudAppInfoBasicPanel extends AbstractSpringCloudAppInfoPanel {
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private AzureTextInput textName;
    private JRadioButton useJava8;
    private JRadioButton useJava11;
    private JRadioButton useJava17;
    private JLabel lblRuntime;
    private TitledSeparator sectionConfiguration;
    private JLabel lblSubscription;

    public SpringCloudAppInfoBasicPanel(@Nullable final SpringCloudCluster cluster) {
        super(cluster);
        $$$setupUI$$$();
        this.init();
        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
    }

    protected void onAppChanged(SpringCloudApp app) {
        final Sku sku = app.getParent().getSku();
        final boolean enterprise = sku.name().startsWith("e");
        this.useJava8.setVisible(!enterprise);
        this.useJava11.setVisible(!enterprise);
        this.useJava17.setVisible(!enterprise);
        this.lblRuntime.setVisible(!enterprise);
        this.sectionConfiguration.setVisible(!enterprise);
    }

    @Override
    public SpringCloudAppConfig getValue() {
        final SpringCloudAppConfig config = super.getValue();
        if (this.useJava17.isVisible()) {
            final String javaVersion = this.useJava17.isSelected() ? RuntimeVersion.JAVA_17.toString() :
                this.useJava11.isSelected() ? RuntimeVersion.JAVA_11.toString() : RuntimeVersion.JAVA_8.toString();
            final SpringCloudDeploymentConfig deployment = config.getDeployment();
            deployment.setRuntimeVersion(javaVersion);
        }
        return config;
    }

    @Override
    public void setValue(final SpringCloudAppConfig config) {
        super.setValue(config);
        final SpringCloudDeploymentConfig deployment = config.getDeployment();
        this.useJava17.setSelected(StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), RuntimeVersion.JAVA_17.toString()));
        this.useJava11.setSelected(StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), RuntimeVersion.JAVA_11.toString()));
        this.useJava8.setSelected(StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), RuntimeVersion.JAVA_8.toString()));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
            this.getTextName(),
            this.getSelectorSubscription(),
            this.getSelectorCluster()
        );
    }
}

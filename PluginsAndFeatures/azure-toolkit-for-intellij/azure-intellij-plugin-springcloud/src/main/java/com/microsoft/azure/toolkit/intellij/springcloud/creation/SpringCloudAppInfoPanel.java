/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Getter(AccessLevel.PROTECTED)
public abstract class SpringCloudAppInfoPanel extends JPanel implements AzureFormPanel<SpringCloudAppDraft> {
    private static final String SPRING_CLOUD_APP_NAME_PATTERN = "^[a-z][a-z0-9-]{2,30}[a-z0-9]$";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    @Nullable
    private final SpringCloudCluster cluster;
    private final String defaultAppName;
    private SpringCloudAppDraft draft;

    public SpringCloudAppInfoPanel(@Nullable final SpringCloudCluster cluster) {
        super();
        this.cluster = cluster;
        this.defaultAppName = String.format("spring-app-%s", DATE_FORMAT.format(new Date()));
    }

    protected void init() {
        final SubscriptionComboBox selectorSubscription = this.getSelectorSubscription();
        final SpringCloudClusterComboBox selectorCluster = this.getSelectorCluster();
        final AzureTextInput textName = this.getTextName();
        selectorSubscription.setRequired(true);
        selectorSubscription.addItemListener(this::onSubscriptionChanged);
        selectorCluster.setRequired(true);
        selectorCluster.addItemListener(this::onClusterChanged);
        textName.setRequired(true);
        textName.setValue(this.defaultAppName);
        textName.addValidator(() -> {
            try {
                validateSpringCloudAppName(textName.getValue(), this.cluster);
            } catch (final IllegalArgumentException e) {
                final AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(textName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
            }
            return AzureValidationInfo.success(this);
        });
        if (Objects.nonNull(this.cluster)) {
            selectorSubscription.setValue(this.cluster.getSubscription());
            selectorCluster.setValue(this.cluster);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Subscription subscription = this.getSelectorSubscription().getValue();
            this.getSelectorCluster().setSubscription(subscription);
        }
    }

    private void onClusterChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final SpringCloudCluster c = this.getSelectorCluster().getValue();
            final String appName = StringUtils.firstNonBlank(this.getTextName().getName(), this.defaultAppName);
            if (Objects.nonNull(c)) {
                final SpringCloudApp app = c.apps().create(appName, c.getResourceGroupName());
                this.onAppChanged(app);
            }
        }
    }

    protected void onAppChanged(SpringCloudApp app) {
        if (Objects.isNull(this.draft)) {
            this.draft = (SpringCloudAppDraft) (app.isDraft() ? app : app.update());
            AzureTaskManager.getInstance().runLater(() -> this.setValue(this.draft), AzureTask.Modality.ANY);
        }
    }

    @Override
    public SpringCloudAppDraft getValue() {
        final SpringCloudCluster cluster = Optional.ofNullable(this.getSelectorCluster().getValue()).orElse(this.cluster);
        final String appName = this.getTextName().getValue();
        return Optional.ofNullable(this.draft)
            .orElseGet(() -> cluster.apps().create(appName, cluster.getResourceGroupName()));
    }

    @Override
    public synchronized void setValue(final SpringCloudAppDraft app) {
        this.draft = app;
        this.getTextName().setValue(app.getName());
        this.getSelectorCluster().setValue(app.getParent());
        this.getSelectorSubscription().setValue(app.getSubscription());
    }

    @Override
    public void setVisible(final boolean visible) {
        this.getContentPanel().setVisible(visible);
        super.setVisible(visible);
    }

    public static void validateSpringCloudAppName(final String name, final SpringCloudCluster cluster) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException(AzureMessageBundle.message("springcloud.app.name.validate.empty").toString());
        } else if (!name.matches(SPRING_CLOUD_APP_NAME_PATTERN)) {
            throw new IllegalArgumentException(AzureMessageBundle.message("springcloud.app.name.validate.invalid").toString());
        } else if (Objects.nonNull(cluster) && Objects.nonNull(cluster.apps().get(name, cluster.getResourceGroupName()))) {
            throw new IllegalArgumentException(AzureMessageBundle.message("springcloud.app.name.validate.exist", name).toString());
        }
    }

    protected abstract SubscriptionComboBox getSelectorSubscription();

    protected abstract SpringCloudClusterComboBox getSelectorCluster();

    protected abstract AzureTextInput getTextName();

    protected abstract JPanel getContentPanel();
}

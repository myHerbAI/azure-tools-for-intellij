/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.creation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.eclipse.common.form.AzureFormPanel;
import com.microsoft.azure.toolkit.eclipse.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractSpringCloudAppInfoPanel extends Composite implements AzureFormPanel<SpringCloudAppConfig> {
    private static final String SPRING_CLOUD_APP_NAME_PATTERN = "^[a-z][a-z0-9-]{2,30}[a-z0-9]$";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    @Nullable
    private final SpringCloudCluster cluster;
    private final String defaultAppName;
    private SpringCloudAppConfig originalConfig;

    public AbstractSpringCloudAppInfoPanel(Composite parent, @Nullable final SpringCloudCluster cluster) {
        super(parent, SWT.NONE);
        this.cluster = cluster;
        this.defaultAppName = String.format("spring-app-%s", DATE_FORMAT.format(new Date()));
    }

    protected void init() {
        final SubscriptionComboBox selectorSubscription = this.getSelectorSubscription();
        final SpringCloudClusterComboBox selectorCluster = this.getSelectorCluster();
        final AzureTextInput textName = this.getTextName();
        selectorSubscription.setRequired(true);
        selectorSubscription.addValueChangedListener(this::onSubscriptionChanged);
        selectorCluster.setRequired(true);
        selectorCluster.addValueChangedListener(this::onClusterChanged);
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
            selectorSubscription.setValue(new ItemReference<>(this.cluster.getSubscriptionId(), Subscription::getId));
            selectorCluster.setValue(new ItemReference<>(this.cluster.name(), AzResource::getName));
        }
    }

    private void onSubscriptionChanged(@Nullable final Subscription subscription) {
        this.getSelectorCluster().setSubscription(subscription);
    }

    private void onClusterChanged(@Nullable final SpringCloudCluster c) {
        final String appName = StringUtils.firstNonBlank(this.getTextName().getValue(), this.defaultAppName);
        if (Objects.nonNull(c)) {
            final SpringCloudApp app = c.apps().updateOrCreate(appName, c.getResourceGroupName());
            this.onAppChanged(app);
        }
    }

    protected void onAppChanged(SpringCloudApp app) {
        if (Objects.isNull(this.originalConfig)) {
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                this.originalConfig = SpringCloudAppConfig.fromApp(app);
                AzureTaskManager.getInstance().runLater(() -> this.setValue(this.originalConfig));
            });
        }
    }

    protected SpringCloudAppConfig getValue(SpringCloudAppConfig config) {
        config.setSubscriptionId(Optional.ofNullable(this.getSelectorSubscription().getValue()).map(Subscription::getId).orElse(null));
        config.setClusterName(Optional.ofNullable(this.getSelectorCluster().getValue()).map(AzResource::getName).orElse(null));
        config.setAppName(this.getTextName().getValue());
        return config;
    }

    public SpringCloudAppConfig getValue() {
        final SpringCloudAppConfig config = Optional.ofNullable(this.originalConfig)
            .orElse(SpringCloudAppConfig.builder().deployment(SpringCloudDeploymentConfig.builder().build()).build());
        return getValue(config);
    }

    @Override
    public synchronized void setValue(final SpringCloudAppConfig config) {
        final Integer count = config.getDeployment().getCapacity();
        config.getDeployment().setCapacity(Objects.isNull(count) || count == 0 ? 1 : count);
        this.originalConfig = config;
        this.getTextName().setValue(config.getAppName());
        if (Objects.nonNull(config.getClusterName())) {
            this.getSelectorCluster().setValue(new ItemReference<>(config.getClusterName(), AzResource::getName));
        }
        if (Objects.nonNull(config.getSubscriptionId())) {
            this.getSelectorSubscription().setValue(new ItemReference<>(config.getSubscriptionId(), Subscription::getId));
        }
    }

    @Override
    public void setVisible(final boolean visible) {
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
}

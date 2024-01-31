/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceIntelliJActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppInfoPanel.QUICK_START_IMAGE;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class FunctionAppCreationDialog extends ConfigDialog<FunctionAppConfig> {

    private JPanel contentPane;
    private AppServiceInfoBasicPanel<FunctionAppConfig> basicPanel;
    private FunctionAppAdvancedConfigPanel advancePanel;

    public FunctionAppCreationDialog(final Project project) {
        super(project);
        this.init();
        setFrontPanel(basicPanel);
    }

    @Override
    protected AzureFormPanel<FunctionAppConfig> getAdvancedFormPanel() {
        return advancePanel;
    }

    @Override
    protected AzureFormPanel<FunctionAppConfig> getBasicFormPanel() {
        return basicPanel;
    }

    @Override
    protected String getDialogTitle() {
        return message("function.create.dialog.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @ExceptionNotification
    private void createUIComponents() {
        // TODO: place custom component creation code here
        final List<Subscription> selectedSubscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        if (selectedSubscriptions.isEmpty()) {
            this.close();
            throw new AzureToolkitRuntimeException("there are no subscriptions selected in your account.", IAccountActions.SELECT_SUBS);
        }
        basicPanel = new AppServiceInfoBasicPanel<>(project, () -> AppServiceIntelliJActionsContributor.getDefaultFunctionAppConfig(null)) {
            @Override
            public FunctionAppConfig getValue() {
                // Create AI instance with same name by default
                final FunctionAppConfig config = super.getValue();
                Optional.ofNullable(config).map(FunctionAppConfig::applicationInsightsConfig).ifPresent(insightConfig -> {
                    if (insightConfig.getCreateNewInstance() && !StringUtils.equals(insightConfig.getName(), config.appName())) {
                        insightConfig.setName(config.appName());
                    }
                });

                final OperatingSystem os = Optional.ofNullable(config)
                        .map(FunctionAppConfig::runtime).map(RuntimeConfig::os).orElse(null);
                final PricingTier pricingTier = Optional.ofNullable(config)
                        .map(FunctionAppConfig::getPricingTier).orElse(null);
                final Boolean isConsumption = Optional.ofNullable(pricingTier)
                        .map(tier -> tier.isConsumption() || tier.isFlexConsumption()).orElse(false);
                // set draft plan pricing tier to premium if runtime is docker
                if (os == OperatingSystem.DOCKER && isConsumption) {
                    Optional.of(config)
                            .map(AppServiceConfig::getServicePlanConfig)
                            .map(AppServicePlanConfig::getAppServicePlan)
                            .filter(AppServicePlan::isDraftForCreating).ifPresent(ignore -> {
                                config.pricingTier(PricingTier.PREMIUM_P1V2);
                            });
                }
                return config;
            }
        };
        basicPanel.getSelectorRuntime().setPlatformList(FunctionAppRuntime.getMajorRuntimes());
        basicPanel.setDefaultImage(QUICK_START_IMAGE.getFullImageName());
        advancePanel = new FunctionAppAdvancedConfigPanel(project);
    }
}

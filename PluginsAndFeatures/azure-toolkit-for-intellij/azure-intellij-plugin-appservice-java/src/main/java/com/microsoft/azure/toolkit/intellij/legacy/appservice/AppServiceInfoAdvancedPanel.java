/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azuretools.utils.WebAppUtils;
import lombok.Getter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

@Getter
public class AppServiceInfoAdvancedPanel<T extends AppServiceConfig> extends JPanel implements AzureFormPanel<T> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final String NOT_APPLICABLE = "N/A";
    private final Project project;
    private final Supplier<? extends T> supplier;
    private T config;

    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private ResourceGroupComboBox selectorGroup;
    private AppNameInput textName;
    private RuntimeComboBox selectorRuntime;
    private RegionComboBox selectorRegion;
    private JLabel textSku;
    private AzureArtifactComboBox selectorApplication;
    private ServicePlanComboBox selectorServicePlan;
    private TitledSeparator deploymentTitle;
    private JLabel lblArtifact;
    private JLabel lblSubscription;
    private JLabel lblResourceGroup;
    private JLabel lblName;
    private JLabel lblPlatform;
    private JLabel lblRegion;
    private JLabel lblAppServicePlan;
    private JLabel lblSku;

    public AppServiceInfoAdvancedPanel(final Project project, final Supplier<? extends T> supplier) {
        super();
        this.project = project;
        this.supplier = supplier;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public T getValue() {
        final T result = this.config == null ? supplier.get() : this.config;
        result.appName(textName.getValue());
        result.region(selectorRegion.getValue());
        Optional.ofNullable(selectorSubscription.getValue()).map(Subscription::getId).ifPresent(result::subscriptionId);
        Optional.ofNullable(selectorGroup.getValue()).map(ResourceGroup::getName).ifPresent(result::resourceGroup);
        Optional.ofNullable(selectorRuntime.getValue()).map(RuntimeConfig::fromRuntime).ifPresent(result::runtime);
        Optional.ofNullable(selectorServicePlan.getValue()).map(AppServicePlan::getName).ifPresent(result::servicePlanName);
        Optional.ofNullable(selectorServicePlan.getValue()).map(AppServicePlan::getPricingTier).ifPresent(result::pricingTier);
        Optional.ofNullable(selectorServicePlan.getValue()).map(AppServicePlan::getResourceGroup)
                .map(ResourceGroup::getResourceGroupName).ifPresentOrElse(result::servicePlanResourceGroup, () -> result.servicePlanResourceGroup(result.resourceGroup()));
//        if (Objects.nonNull(artifact)) {
//            final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
//            final String path = artifact.getFileForDeployment();
//            config.setApplication(Paths.get(path));
//        }
        this.config = result;
        return result;
    }

    @Override
    public void setValue(final T config) {
        this.config = config;
        final Subscription subscription = Optional.ofNullable(config.subscriptionId())
                                                  .map(Azure.az(AzureAccount.class).account()::getSubscription)
                                                  .orElse(null);
        Optional.ofNullable(subscription).ifPresent(selectorSubscription::setValue);
        this.textName.setValue(config.appName());
        this.textName.setSubscription(subscription);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.selectorRegion.setValue(config.region());
            Optional.ofNullable(AppServiceConfig.getResourceGroup(config)).ifPresent(this.selectorGroup::setValue);
            Optional.ofNullable(AppServiceConfig.getServicePlanConfig(config)).map(AppServicePlanConfig::getAppServicePlan).ifPresent(this.selectorServicePlan::setValue);
            Optional.ofNullable(config.runtime())
                    .map(c -> config instanceof FunctionAppConfig ? RuntimeConfig.toFunctionAppRuntime(c) : RuntimeConfig.toWebAppRuntime(c))
                    .ifPresent(this.selectorRuntime::setValue);
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorSubscription,
            this.selectorGroup,
            this.selectorRuntime,
            this.selectorRegion,
            this.selectorApplication,
            this.selectorServicePlan
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.lblArtifact.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }

    private void init() {
        final String date = DATE_FORMAT.format(new Date());
        final String defaultWebAppName = String.format("app-%s-%s", this.project.getName(), date);
        this.textName.setValue(defaultWebAppName);
        this.textSku.setBorder(JBUI.Borders.emptyLeft(5));
        this.textSku.setText(NOT_APPLICABLE);
        this.selectorServicePlan.addItemListener(this::onServicePlanChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorRuntime.addItemListener(this::onRuntimeChanged);
        this.selectorRegion.addItemListener(this::onRegionChanged);
        this.selectorGroup.addItemListener(this::onGroupChanged);
        this.textName.setRequired(true);
        this.selectorServicePlan.setRequired(true);
        this.selectorSubscription.setRequired(true);
        this.selectorGroup.setRequired(true);
        this.selectorRuntime.setRequired(true);
        this.selectorRegion.setRequired(true);

        this.lblSubscription.setLabelFor(selectorSubscription);
        this.lblResourceGroup.setLabelFor(selectorGroup);
        this.lblName.setLabelFor(textName);
        this.lblPlatform.setLabelFor(selectorRuntime);
        this.lblRegion.setLabelFor(selectorRegion);
        this.lblAppServicePlan.setLabelFor(selectorServicePlan);
        this.lblArtifact.setLabelFor(selectorApplication);
        this.selectorApplication.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            final Runtime runtime = this.selectorRuntime.getValue();
            return StringUtils.isNotBlank(ext) && (runtime == null || WebAppUtils.isSupportedArtifactType(runtime, ext));
        });

        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
        this.lblResourceGroup.setIcon(AllIcons.General.ContextHelp);
        this.lblAppServicePlan.setIcon(AllIcons.General.ContextHelp);
    }

    private void onGroupChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            this.selectorServicePlan.setResourceGroup((ResourceGroup) e.getItem());
        }
    }

    private void onRegionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Region region = (Region) e.getItem();
            this.selectorServicePlan.setRegion(region);
        }
    }

    private void onRuntimeChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Runtime runtime = (Runtime) e.getItem();
            final OperatingSystem operatingSystem = Objects.isNull(runtime) ? null :
                    // Docker runtime use Linux service plan too
                    runtime.isWindows() ? OperatingSystem.WINDOWS : OperatingSystem.LINUX;
            this.selectorServicePlan.setOperatingSystem(operatingSystem);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        //TODO: @wangmi try subscription mechanism? e.g. this.selectorGroup.subscribe(this.selectSubscription)
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.selectorGroup.setSubscription(subscription);
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.selectorServicePlan.setSubscription(subscription);
        }
    }

    private void onServicePlanChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final AppServicePlan plan = (AppServicePlan) e.getItem();
            if (plan == null || plan.getPricingTier() == null) {
                return;
            }
            final String pricing = Objects.equals(plan.getPricingTier(), PricingTier.CONSUMPTION) ?
                    "Consumption" : String.format("%s_%s", plan.getPricingTier().getTier(), plan.getPricingTier().getSize());
            this.textSku.setText(pricing);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.textSku.setText(NOT_APPLICABLE);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project, true);
        this.selectorApplication.reloadItems();
    }

    public void setValidPricingTier(List<PricingTier> pricingTier, PricingTier defaultPricingTier) {
        selectorServicePlan.setValidPricingTierList(pricingTier, defaultPricingTier);
    }

    public void setValidRuntime(List<? extends Runtime> runtimes) {
        selectorRuntime.setPlatformList(runtimes);
    }

    public void setFixedRuntime(final Runtime runtime) {
        selectorRuntime.setPlatformList(Collections.singletonList(runtime));
        lblPlatform.setVisible(false);
        selectorRuntime.setEditable(false);
    }
}

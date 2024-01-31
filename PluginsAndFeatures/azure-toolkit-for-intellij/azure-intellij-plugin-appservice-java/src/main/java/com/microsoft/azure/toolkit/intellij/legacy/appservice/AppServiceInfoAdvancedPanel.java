/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.appservice.DockerUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
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
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azuretools.utils.WebAppUtils;
import lombok.Getter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

@Getter
public class AppServiceInfoAdvancedPanel<T extends AppServiceConfig> extends JPanel implements AzureFormPanel<T> {
    public static final ContainerAppDraft.ImageConfig QUICK_START_IMAGE =
            new ContainerAppDraft.ImageConfig("mcr.microsoft.com/azuredocs/containerapps-helloworld:latest");
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
    private TitledSeparator imageTitle;
    private JPanel pnlImage;
    private JPanel pnlImageContainer;
    private JLabel lblQuickStart;
    private JCheckBox chkUseQuickStart;
    private ImageForm pnlContainer;

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
        Optional.ofNullable(selectorServicePlan.getValue())
                .map(plan -> plan.isDraftForCreating() ? result.getResourceGroup() : plan.getResourceGroupName())
                .ifPresent(result::servicePlanResourceGroup);
        final Boolean isDocker = Optional.ofNullable(selectorRuntime.getValue()).map(Runtime::isDocker).orElse(false);
        if (isDocker) {
            final ContainerAppDraft.ImageConfig image = chkUseQuickStart.isSelected() ? QUICK_START_IMAGE : pnlContainer.getValue();
            Optional.ofNullable(image).map(DockerUtils::convertImageConfigToRuntimeConfig).ifPresent(config::runtime);
        }

        if (deploymentTitle.isVisible()) {
            Optional.ofNullable(selectorApplication.getValue()).map(AzureArtifact::getFileForDeployment)
                    .map(File::new)
                    .filter(File::exists)
                    .ifPresent(result::file);
        }
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
                    .map(RuntimeConfig::toWebAppRuntime)
                    .ifPresent(this.selectorRuntime::setValue);
            Optional.ofNullable(config.file())
                    .filter(File::exists)
                    .map(f -> AzureArtifact.createFromFile(f.getPath(), project))
                    .ifPresent(selectorApplication::setValue);
            final ContainerAppDraft.ImageConfig imageConfig = DockerUtils.convertRuntimeConfigToImageConfig(config.getRuntime());
            final boolean useDefaultImage = Objects.isNull(imageConfig) || StringUtils.equalsIgnoreCase(QUICK_START_IMAGE.getFullImageName(), imageConfig.getFullImageName());
            chkUseQuickStart.setSelected(useDefaultImage);
            toggleImageType(useDefaultImage);
            Optional.ofNullable(imageConfig).ifPresent(pnlContainer::setValue);
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
        this.selectorServicePlan.setValidPricingTierList(new ArrayList<>(PricingTier.WEB_APP_PRICING), WebAppConfig.DEFAULT_PRICING_TIER);
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

        lblQuickStart.setLabelFor(chkUseQuickStart);
        chkUseQuickStart.addItemListener(ignore -> toggleImageType(chkUseQuickStart.isSelected()));

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

            final boolean isDocker = Optional.ofNullable(runtime).map(Runtime::isDocker).orElse(false);
            this.imageTitle.setVisible(isDocker);
            this.pnlImage.setVisible(isDocker);
            this.deploymentTitle.setVisible(!isDocker);
            this.lblArtifact.setVisible(!isDocker);
            this.selectorApplication.setVisible(!isDocker);
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

        // TODO: place custom component creation code here
        this.pnlImageContainer = new JPanel(new GridLayoutManager(1, 1));
        this.pnlContainer = new ImageForm();
        this.pnlImageContainer.add(this.pnlContainer.getContentPanel(), new GridConstraints(0, 0, 1, 1, 0,
                GridConstraints.FILL_BOTH, 7, 7, null, null, null, 0));
    }

    public void setFixedRuntime(final Runtime runtime) {
        selectorRuntime.setPlatformList(Collections.singletonList(runtime));
    }

    private void toggleImageType(final boolean useQuickStart){
        pnlContainer.setVisible(!useQuickStart);
    }
}

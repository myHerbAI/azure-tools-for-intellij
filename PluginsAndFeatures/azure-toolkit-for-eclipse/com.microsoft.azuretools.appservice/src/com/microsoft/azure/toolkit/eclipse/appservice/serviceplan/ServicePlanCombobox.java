/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice.serviceplan;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServicePlanCombobox extends AzureComboBox<AppServicePlan> {
    private Subscription subscription;
    private final List<AppServicePlanDraft> localItems = new ArrayList<>();
    private List<PricingTier> pricingTierList = new ArrayList<>(PricingTier.WEB_APP_PRICING);
    private PricingTier defaultPricingTier = PricingTier.BASIC_B2;
    private Predicate<AppServicePlan> servicePlanFilter;

    public void setOs(OperatingSystem os) {
        if (os == this.os) {
            return;
        }
        this.os = os;
        if (os == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    private OperatingSystem os;
    private Region region;

    public ServicePlanCombobox(Composite parent) {
        super(parent);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        // Clean up app service plan cache when switch subscription
        // todo: leverage event hub to update resource cache automatically
        try {
            CacheManager.evictCache("appservice/{}/plans", subscription.getId());
        } catch (ExecutionException e) {
            // swallow exception while clean up cache
        }
        this.refreshItems();
    }

    public void setValidPricingTierList(@Nonnull final List<PricingTier> pricingTierList, @Nonnull final PricingTier defaultPricingTier) {
        this.pricingTierList = pricingTierList;
        this.defaultPricingTier = defaultPricingTier;
        this.servicePlanFilter = appServicePlan -> pricingTierList.contains(appServicePlan.getPricingTier());
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final AppServicePlan plan = (AppServicePlan) item;
        if (plan.isDraftForCreating()) {
            return "(New) " + plan.getName();
        }
        return plan.getName();
    }

    @Nonnull
    @Override
    @AzureOperation(
            name = "internal/appservice.list_plans.subscription|region|os",
            params = {"this.subscription.getId()", "this.region.getName()", "this.os.name()"}
    )
    protected List<? extends AppServicePlan> loadItems() throws Exception {
        final List<AppServicePlan> plans = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.localItems)) {
                plans.addAll(this.localItems.stream()
                    .filter(p -> this.subscription.equals(p.getSubscription()))
                    .collect(Collectors.toList()));
            }
            final List<AppServicePlan> remotePlans = Azure.az(AzureAppService.class).plans(subscription.getId()).list();
            plans.addAll(remotePlans);
            Stream<AppServicePlan> stream = plans.stream();
            if (Objects.nonNull(this.region)) {
                stream = stream.filter(p -> Objects.equals(p.getRegion(), this.region.getLabel()));
            }
            if (Objects.nonNull(this.os)) {
                stream = stream.filter(p -> p.getOperatingSystem() == this.os);
            }
            if (Objects.nonNull(this.servicePlanFilter)) {
                stream = stream.filter(servicePlanFilter);
            }
            stream = stream.sorted((first, second) -> StringUtils.compare(first.getName(), second.getName()));
            plans.addAll(stream.collect(Collectors.toList()));
            return plans;
        }
        return plans;
    }

    protected Control getExtension() {
        Button button = new Button(this, SWT.NONE);
        button.setText("Create");
        button.setToolTipText("Create new app service plan");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                ServicePlanCreationDialog dialog = new ServicePlanCreationDialog(ServicePlanCombobox.this.getShell());
                dialog.setOs(ServicePlanCombobox.this.os);
                dialog.setRegion(ServicePlanCombobox.this.region);
                dialog.setSubscription(ServicePlanCombobox.this.subscription);
                dialog.setPricingTier(new ArrayList<>(PricingTier.WEB_APP_PRICING));
                if (dialog.open() == Window.OK) {
                    AppServicePlanDraft plan = dialog.getData();
                    localItems.add(0, plan);
                    refreshItems();
                    setValue(plan);
                }

            }
        });
        return button;
    }

    public void setAzureRegion(Region region) {
        this.region = region;

        this.refreshItems();
    }
}

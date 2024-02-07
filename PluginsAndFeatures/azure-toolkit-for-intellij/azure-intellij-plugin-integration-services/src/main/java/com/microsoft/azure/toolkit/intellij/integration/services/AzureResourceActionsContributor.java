package com.microsoft.azure.toolkit.intellij.integration.services;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

import java.util.Objects;

public class AzureResourceActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final Action.Id<Azure> ADD_RESOURCE = Action.Id.of("user/servicesview.add_resource");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> REMOVE_RESOURCE = Action.Id.of("user/servicesview.remove_resource");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(ADD_RESOURCE)
            .withIcon(AzureIcons.Action.ADD.getIconPath())
            .withLabel("Add Resource...")
            .withHandler(s -> {
                final AzureResourceManager manager = AzureResourceManager.getInstance();
                final AbstractAzResource<?, ?, ?> resource = Azure.az().getById("/subscriptions/685ba005-af8d-4b04-8f16-a7bf38b2eb5a/resourceGroups/rg-app-240131155805/providers/Microsoft.Web/sites/hanli-test-default-docker");
                if (Objects.nonNull(resource)) {
                    manager.addResource(resource);
                }
            })
            .register(am);
        new Action<>(REMOVE_RESOURCE)
            .withIcon(AzureIcons.Action.REMOVE.getIconPath())
            .withLabel("Remove Resource")
            .withHandler(s -> AzureResourceManager.getInstance().removeResource(s))
            .register(am);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}

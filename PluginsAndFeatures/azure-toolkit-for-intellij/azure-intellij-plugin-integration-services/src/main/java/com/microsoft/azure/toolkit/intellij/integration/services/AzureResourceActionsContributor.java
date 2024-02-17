package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

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
            .withHandler((s, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final AnActionEvent event = (AnActionEvent) e;
                final AzureResourceSelectDialog dialog = new AzureResourceSelectDialog(event.getProject());
                dialog.setOkActionListener((resources) -> {
                    dialog.close();
                    if (Objects.nonNull(resources)) {
                        final AzureResourceManager manager = AzureResourceManager.getInstance();
                        manager.addResources(resources);
                    }
                });
                dialog.show();
            }))
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

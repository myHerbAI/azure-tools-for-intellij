package com.microsoft.azure.toolkit.ide.common.component;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ServiceLinkerNode extends Node<ServiceLinker> {

    public ServiceLinkerNode(@Nonnull ServiceLinker linker) {
        super(linker);
        this.withActions(ResourceCommonActionsContributor.SERVICE_LINKER_ACTIONS);
        this.addInlineAction(ResourceCommonActionsContributor.FOCUS_ON_CONNECTED_SERVICE);
    }

    @Override
    public String buildLabel() {
        final ServiceLinker linker = this.getValue();
        final ResourceId resourceId = ResourceId.fromString(linker.getTargetServiceId());
        return Optional.ofNullable(resourceId.name()).orElse(linker.getName());
    }

    @Override
    public AzureIcon buildIcon() {
        final ResourceId resourceId = ResourceId.fromString(this.getValue().getTargetServiceId());
        return AzureIcon.builder().iconPath(String.format("/icons/%s/default.svg", resourceId.fullResourceType())).build();
    }
}

package com.microsoft.azure.toolkit.ide.common.component;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class ServiceLinkerNode extends Node<ServiceLinker> {
    public ServiceLinkerNode(@Nonnull ServiceLinker data) {
        super(data);
        this.view(new ServiceLinkerNodeView(data));
        this.actions(ResourceCommonActionsContributor.SERVICE_LINKER_ACTIONS);
        this.addInlineAction(ResourceCommonActionsContributor.FOCUS_ON_CONNECTED_SERVICE);
    }

    static class ServiceLinkerNodeView implements NodeView {
        @Nonnull
        @Getter
        private final ServiceLinker serviceLinker;
        private final AzureEventBus.EventListener listener;
        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        ServiceLinkerNodeView(@Nonnull ServiceLinker serviceLinker) {
            this.serviceLinker = serviceLinker;
            this.listener = new AzureEventBus.EventListener(this::onEvent);
            AzureEventBus.on("resource.refreshed.resource", listener);
            this.refreshView();
        }

        @Override
        public String getLabel() {
            try {
                final ResourceId resourceId = ResourceId.fromString(this.serviceLinker.getTargetServiceId());
                return Optional.ofNullable(resourceId.name()).orElse(serviceLinker.getName());
            } catch (final Exception ignored) {}
            return serviceLinker.getName();
        }

        @Override
        public String getIconPath() {
            String iconPath = AzureIcons.Resources.GENERIC_RESOURCE.getIconPath();
            try {
                final ResourceId resourceId = ResourceId.fromString(this.serviceLinker.getTargetServiceId());
                if (StringUtils.isNotBlank(resourceId.fullResourceType())) {
                    iconPath = String.format("/icons/%s/default.svg", resourceId.fullResourceType());
                }
            } catch (final Exception ignored) {}
            return iconPath;
        }

        @Override
        public String getDescription() {
            return serviceLinker.getStatus();
        }

        @Override
        public void dispose() {
            AzureEventBus.off("resource.refreshed.resource", listener);
            this.refresher = null;
        }

        private void onEvent(AzureEvent event) {
            final Object source = event.getSource();
            if (source instanceof ServiceLinker && ((AzResource) source).getId().equals(serviceLinker.getId())) {
                AzureTaskManager.getInstance().runLater(this::refreshChildren);
            }
        }
    }
}

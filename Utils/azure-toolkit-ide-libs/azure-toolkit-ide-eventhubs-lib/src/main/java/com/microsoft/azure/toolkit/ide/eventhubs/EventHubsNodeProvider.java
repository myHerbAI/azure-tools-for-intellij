package com.microsoft.azure.toolkit.ide.eventhubs;

import com.azure.resourcemanager.eventhubs.models.EntityStatus;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.eventhubs.AzureEventHubsNamespace;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsInstance;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsNamespace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventHubsNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Event Hubs";
    private static final String ICON = AzureIcons.EventHubs.MODULE.getIconPath();
    private static final AzureIconProvider<EventHubsInstance> EVENT_HUBS_ICON_PROVIDER =
            new AzureResourceIconProvider<EventHubsInstance>()
                    .withModifier(EventHubsNodeProvider::getSendDisabledModifier);

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureEventHubsNamespace.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureEventHubsNamespace ||
                data instanceof EventHubsNamespace || data instanceof EventHubsInstance;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureEventHubsNamespace) {
            return new AzServiceNode<>((AzureEventHubsNamespace) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(EventHubsActionsContributor.SERVICE_ACTIONS)
                .addChildren(this::listEventHubNamespaces, (eventHubsNamespace, eventHubsNamespaceModule) ->
                    this.createNode(eventHubsNamespace, eventHubsNamespaceModule, manager));
        } else if (data instanceof EventHubsNamespace) {
            return new AzResourceNode<>((EventHubsNamespace) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                .withActions(EventHubsActionsContributor.NAMESPACE_ACTIONS)
                .addChildren(c -> Optional.ofNullable(c).map(EventHubsNamespace::getInstances).orElse(Collections.emptyList()),
                    (eventHubsInstance, eventHubNode) -> this.createNode(eventHubsInstance, eventHubNode, manager));
        } else if (data instanceof EventHubsInstance) {
            return new AzResourceNode<>((EventHubsInstance) data)
                .withIcon(EVENT_HUBS_ICON_PROVIDER::getIcon)
                .withActions(EventHubsActionsContributor.INSTANCE_ACTIONS);
        }
        return null;
    }

    private List<EventHubsNamespace> listEventHubNamespaces(final AzureEventHubsNamespace azureEventHubsNamespace) {
        return azureEventHubsNamespace.list().stream().flatMap(m -> m.eventHubsNamespaces().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    private static AzureIcon.Modifier getSendDisabledModifier(EventHubsInstance resource) {
        return resource.getEntityStatus() == EntityStatus.SEND_DISABLED ? AzureIcon.Modifier.SEND_DISABLED : null;
    }
}

package com.microsoft.azure.toolkit.ide.synapse.spark;

import com.azure.resourcemanager.hdinsight.models.StorageAccount;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.OpenHDIAzureStorageExplorerAction;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.*;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Refreshable;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SynapseActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final String SERVICE_ACTIONS = "actions.synapse.service";
    public static final String WORKSPACES_NODE_ACTIONS = "actions.synapse.workspace";
    public static final Action.Id<ResourceGroup> GROUP_CREATE_SYNAPSE_SERVICE = Action.Id.of("user/synapse.create_synapse.group");
    public static final Action.Id<AzResource> OPEN_LAUNCH_WORKSPACE = Action.Id.of("user/synapse.open_launch_workspace");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_LAUNCH_WORKSPACE)
                .withLabel("launch workspace")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(resource -> {

                })
                .withShortcut(am.getIDEDefaultShortcuts().edit())
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup workspaceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                this.OPEN_LAUNCH_WORKSPACE
        );
        am.registerGroup(WORKSPACES_NODE_ACTIONS, workspaceActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_SYNAPSE_SERVICE);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }

}

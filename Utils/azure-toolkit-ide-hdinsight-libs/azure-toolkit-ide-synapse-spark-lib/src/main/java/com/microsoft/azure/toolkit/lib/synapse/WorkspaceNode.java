package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.azure.resourcemanager.hdinsight.models.ClusterGetProperties;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.synapse.models.Workspace;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.SDKAdditionalCluster;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkspaceNode extends AbstractAzResource<WorkspaceNode, SynapseServiceSubscription, Workspace> {

    private ArcadiaWorkSpace workSpace;
//    private final StorageAccountMudule storageAccountMudule;
    /**
     * copy constructor
     */
    protected WorkspaceNode(@Nonnull WorkspaceNode origin) {
        super(origin);
//        this.storageAccountMudule = new StorageAccountMudule(this);
        this.workSpace = origin.workSpace;
    }
//
    protected WorkspaceNode(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull WorkspaceModule module) {
        super(name, resourceGroup, module);
//        this.storageAccountMudule = new StorageAccountMudule(this);
    }
//
    protected WorkspaceNode(@Nonnull Workspace remote, @Nonnull WorkspaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
//        this.storageAccountMudule = new StorageAccountMudule(this);
    }


    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        final ArrayList<AbstractAzResourceModule<?, ?, ?>> modules = new ArrayList<>();
//        modules.add(this.storageAccountMudule);
        return modules;
    }

    @Override
    public String getStatus() {
        return StringUtils.EMPTY;
    }

    @NotNull
    @Override
    protected String loadStatus(@NotNull Workspace remote) {
        return null;
    }

    @Nonnull
    @Override
    public Subscription getSubscription() {
        return super.getSubscription();
    }

    public ArcadiaWorkSpace getWorkSpace() {
        return workSpace;
    }

    public void setWorkSpace(ArcadiaWorkSpace workSpace) {
        this.workSpace = workSpace;
    }

}

package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.azure.resourcemanager.hdinsight.models.Clusters;
import com.azure.resourcemanager.synapse.SynapseManager;
import com.azure.resourcemanager.synapse.models.Workspace;
import com.azure.resourcemanager.synapse.models.Workspaces;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

public class WorkspaceModule extends AbstractAzResourceModule<WorkspaceNode, SynapseServiceSubscription, com.azure.resourcemanager.synapse.models.Workspace> {

    public static final String NAME = "workspaces";

    public WorkspaceModule(@Nonnull SynapseServiceSubscription parent) {
        super(NAME, parent);
    }

    public WorkspaceModule(@NotNull String name, @NotNull SynapseServiceSubscription parent) {
        super(name, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Workspace>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"})
    protected Stream<Workspace> loadResourcesFromAzure() {
        return Optional.ofNullable( this.getClient()).map((c) -> {
            ArcadiaSparkComputeManager manager = new ArcadiaSparkComputeManager();
            List<ArcadiaWorkSpace> workspaces = manager.getWorkspaces().stream().collect(Collectors.toList());


            List<Workspace> sourceList = c.list().iterableByPage().iterator().next().getValue();
            for (Workspace workspace : sourceList) {

            }
            List<Workspace> resultList = new ArrayList<Workspace>();

            // Remove duplicate clusters that share the same cluster name
//            List<IClusterDetail> additionalClusterDetails = ClusterManagerEx.getInstance().getAdditionalClusterDetails();
//            HashSet<String> clusterIdSet = new HashSet<>();
//            for (Cluster cluster : sourceList) {
//                boolean isLinkedCluster = false;
//                for (IClusterDetail additionalCluster : additionalClusterDetails) {
//                    if (additionalCluster.getName().equals(cluster.name()))
//                        isLinkedCluster = true;
//                }
//                if ((!isLinkedCluster) && (clusterIdSet.add(cluster.id()) && isSparkCluster(cluster.properties().clusterDefinition().kind())))
//                    resultList.add(cluster);
//            }
            return sourceList.stream();
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    public Workspaces getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SynapseManager::workspaces).orElse(null);
    }

    @Nullable
    @Override
    public WorkspaceNode get(@Nonnull String name, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(resourceGroup) || StringUtils.equalsIgnoreCase(resourceGroup, RESOURCE_GROUP_PLACEHOLDER)) {
            return this.list().stream().filter(c -> StringUtils.equalsIgnoreCase(name, c.getName())).findAny().orElse(null);
        }
        return super.get(name, resourceGroup);
    }

    @NotNull
    @Override
    protected WorkspaceNode newResource(@NotNull Workspace workspace) {
        return new WorkspaceNode(workspace,this);
    }

    @NotNull
    @Override
    protected WorkspaceNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new WorkspaceNode(name, Objects.requireNonNull(resourceGroupName),this);
    }


    @Override
    @Nonnull
    public String getSubscriptionId() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return super.getSubscriptionId();
        } else {
            return "[LinkedCluster]";
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Synapse Workspaces";
    }

}
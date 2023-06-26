package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.azure.resourcemanager.hdinsight.models.ClusterGetProperties;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.SDKAdditionalCluster;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SparkClusterNode extends AbstractAzResource<SparkClusterNode, HDInsightServiceSubscription, Cluster> {

    private IClusterDetail clusterDetail;
    private final StorageAccountMudule storageAccountMudule;
    /**
     * copy constructor
     */
    protected SparkClusterNode(@Nonnull SparkClusterNode origin) {
        super(origin);
        this.storageAccountMudule = new StorageAccountMudule(this);
        this.clusterDetail = origin.clusterDetail;
    }

    protected SparkClusterNode(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull SparkClusterModule module) {
        super(name, resourceGroup, module);
        this.storageAccountMudule = new StorageAccountMudule(this);
    }

    protected SparkClusterNode(@Nonnull Cluster remote, @Nonnull SparkClusterModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.storageAccountMudule = new StorageAccountMudule(this);
    }


    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        final ArrayList<AbstractAzResourceModule<?, ?, ?>> modules = new ArrayList<>();
        modules.add(this.storageAccountMudule);
        return modules;
    }

    @Override
    public String getStatus() {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || "[LinkedCluster]".equals(this.getSubscriptionId())) {
            return "Linked";
        } else {
            return super.getStatus();
        }
    }

    @Override
    @Nonnull
    protected Optional<Cluster> remoteOptional() {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || "[LinkedCluster]".equals(this.getSubscriptionId())) {
            return Optional.empty();
        } else {
            return super.remoteOptional();
        }
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull Cluster remote) {
        if (remote instanceof SDKAdditionalCluster)
            return "Linked";

        final ClusterGetProperties p = remote.properties();
        return "(Spark:" +
            p.clusterDefinition().componentVersion().get("Spark") + ")" +
            p.clusterState();
    }

    @Override
    public String getResourceGroupName(){
        if (!Azure.az(AzureAccount.class).isLoggedIn() || "[LinkedCluster]".equals(this.getSubscriptionId())) {
            return "[LinkedCluster]";
        } else {
            return super.getResourceGroupName();
        }
    }

    @Nonnull
    @Override
    public Subscription getSubscription() {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || "[LinkedCluster]".equals(this.getSubscriptionId())) {
            return new Subscription("[LinkedCluster]");
        } else {
            return super.getSubscription();
        }
    }

    public IClusterDetail getClusterDetail() {
        return clusterDetail;
    }

    public void setClusterDetail(IClusterDetail clusterDetail) {
        this.clusterDetail = clusterDetail;
    }

}

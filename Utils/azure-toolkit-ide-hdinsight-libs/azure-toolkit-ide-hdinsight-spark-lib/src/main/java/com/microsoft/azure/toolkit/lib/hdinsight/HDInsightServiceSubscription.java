package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HDInsightServiceSubscription extends AbstractAzServiceSubscription<HDInsightServiceSubscription, HDInsightManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final SparkClusterModule sparkClusterModule;

    protected HDInsightServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureHDInsightService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.sparkClusterModule = new SparkClusterModule(this);
    }

    @Nonnull
    public SparkClusterModule clusters() {
        return this.sparkClusterModule;
    }

    protected HDInsightServiceSubscription(@Nonnull HDInsightManager manager, @Nonnull AzureHDInsightService service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Override
    @Nonnull
    public String getStatus() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return super.getStatus();
        } else {
            return "Linked";
        }
    }

    @Override
    @Nonnull
    protected Optional<HDInsightManager> remoteOptional() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return Optional.ofNullable(this.getRemote());
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(sparkClusterModule);
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        return subscriptionId;
    }
}

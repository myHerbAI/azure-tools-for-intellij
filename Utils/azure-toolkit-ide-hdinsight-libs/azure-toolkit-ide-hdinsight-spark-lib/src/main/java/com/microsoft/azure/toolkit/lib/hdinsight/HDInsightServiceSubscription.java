package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class HDInsightServiceSubscription extends AbstractAzServiceSubscription<HDInsightServiceSubscription, HDInsightManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final SparkClusterModule sparkClusterModule;

    protected HDInsightServiceSubscription(@NotNull String subscriptionId, @NotNull AzureHDInsightService service) {
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

    @NotNull
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

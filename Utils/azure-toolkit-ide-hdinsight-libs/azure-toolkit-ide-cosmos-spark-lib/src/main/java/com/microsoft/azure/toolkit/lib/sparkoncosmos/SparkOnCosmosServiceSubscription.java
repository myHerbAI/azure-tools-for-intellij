package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.resourcemanager.cosmos.CosmosManager;
import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class SparkOnCosmosServiceSubscription extends AbstractAzServiceSubscription<SparkOnCosmosServiceSubscription, CosmosManager> {
    @Nonnull
    private final String subscriptionId;
    protected SparkOnCosmosServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureSparkOnCosmosService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
    }

    protected SparkOnCosmosServiceSubscription(@Nonnull CosmosManager manager, @Nonnull AzureSparkOnCosmosService service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Override
    @Nonnull
    protected Optional<CosmosManager> remoteOptional() {
        return Optional.ofNullable(this.getRemote());
    }
    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return null;
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        return subscriptionId;
    }

}

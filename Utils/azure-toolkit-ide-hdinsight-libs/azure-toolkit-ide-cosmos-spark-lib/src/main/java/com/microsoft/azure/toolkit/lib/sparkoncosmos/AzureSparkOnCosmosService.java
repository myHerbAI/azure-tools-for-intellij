package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class AzureSparkOnCosmosService extends AbstractAzService<SparkOnCosmosServiceSubscription, SparkOnCosmosManager> {
    public AzureSparkOnCosmosService() {
        super("Microsoft.SparkOnCosmos");
    }
    @NotNull
    @Override
    protected SparkOnCosmosServiceSubscription newResource(@NotNull SparkOnCosmosManager cosmosManager) {
        return null;
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "sparkoncosmosclusters";
    }

    @Override
    public String getServiceNameForTelemetry() {
        return "sparkoncosmos";
    }

}

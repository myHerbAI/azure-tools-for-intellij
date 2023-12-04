package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.SDKAdditionalCluster;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureSynapseService extends AbstractAzService<SynapseServiceSubscription, SynapseManager> {

    public AzureSynapseService() {
        super("Microsoft.Synapse");
    }

    @Nonnull
    @Override
    protected SynapseServiceSubscription newResource(@Nonnull SynapseManager manager) {
        return null;
    }


    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "synapseclusters";
    }

    @Override
    public String getServiceNameForTelemetry() {
        return "synapse";
    }

}
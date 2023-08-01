package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.azure.resourcemanager.synapse.SynapseManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SynapseServiceSubscription extends AbstractAzServiceSubscription<SynapseServiceSubscription, SynapseManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final WorkspaceModule workspaceModule;

    protected SynapseServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureSynapseService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.workspaceModule = new WorkspaceModule(this);
    }

    @Nonnull
    public WorkspaceModule workspaces() {
        return this.workspaceModule;
    }

    protected SynapseServiceSubscription(@Nonnull HDInsightManager manager, @Nonnull AzureSynapseService service) {
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
    protected Optional<SynapseManager> remoteOptional() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return Optional.ofNullable(this.getRemote());
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(null);//sparkClusterModule);
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        return subscriptionId;
    }
}

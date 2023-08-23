package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceNode extends AbstractAzResource<WorkspaceNode, SynapseServiceSubscription, ArcadiaWorkSpace> {

    private final ArcadiaSparkComputeModule arcadiaSparkComputeModule;
    /**
     * copy constructor
     */
    protected WorkspaceNode(@Nonnull WorkspaceNode origin) {
        super(origin);
        this.arcadiaSparkComputeModule = new ArcadiaSparkComputeModule(this);
    }


    protected WorkspaceNode(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull WorkspaceModule module) {
        super(name, resourceGroup, module);
        this.arcadiaSparkComputeModule = new ArcadiaSparkComputeModule(this);
    }

    protected WorkspaceNode(@Nonnull ArcadiaWorkSpace remote, @Nonnull WorkspaceModule module) {
        super(remote.getName(), ResourceId.fromString(remote.getId()).resourceGroupName(), module);
        this.arcadiaSparkComputeModule = new ArcadiaSparkComputeModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        final ArrayList<AbstractAzResourceModule<?, ?, ?>> modules = new ArrayList<>();
        modules.add(this.arcadiaSparkComputeModule);
        return modules;
    }

    public ArcadiaSparkComputeModule getArcadiaSparkComputeModule() {
        return arcadiaSparkComputeModule;
    }

    @Override
    public String getStatus() {
        return StringUtils.EMPTY;
    }

    @NotNull
    @Override
    protected String loadStatus(@NotNull ArcadiaWorkSpace remote) {
        return null;
    }

    @Nonnull
    @Override
    public Subscription getSubscription() {
        return super.getSubscription();
    }

}

package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ArcadiaSparkComputeNode extends AbstractAzResource<ArcadiaSparkComputeNode, WorkspaceNode, ArcadiaSparkCompute> {
    protected ArcadiaSparkComputeNode(@NotNull String name, @NotNull String resourceGroupName, @NotNull AbstractAzResourceModule<ArcadiaSparkComputeNode, WorkspaceNode, ArcadiaSparkCompute> module) {
        super(name, resourceGroupName, module);
    }

    protected ArcadiaSparkComputeNode(@Nonnull ArcadiaSparkCompute remote, @Nonnull ArcadiaSparkComputeModule module) {
        super(remote.getName(), ResourceId.fromString(remote.getId()).resourceGroupName(), module);
    }
    @NotNull
    @Override
    protected String loadStatus(@NotNull ArcadiaSparkCompute remote) {
        return StringUtils.EMPTY;
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.EMPTY_LIST;
    }
}

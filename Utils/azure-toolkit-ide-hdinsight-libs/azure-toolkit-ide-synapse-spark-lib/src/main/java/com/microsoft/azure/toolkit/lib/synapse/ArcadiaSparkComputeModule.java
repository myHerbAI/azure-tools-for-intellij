package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.core.util.paging.ContinuablePage;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

public class ArcadiaSparkComputeModule extends AbstractAzResourceModule<ArcadiaSparkComputeNode,WorkspaceNode, ArcadiaSparkCompute> {

    public static final String NAME = "ArcadiaSparkComputes";

    private WorkspaceNode workspaceNode;

    public ArcadiaSparkComputeModule(@NotNull WorkspaceNode parent) {
        super(NAME, parent);
        this.workspaceNode = parent;
    }

    public ArcadiaSparkComputeModule(@NotNull String name, @NotNull WorkspaceNode parent) {
        super(name, parent);
        this.workspaceNode = parent;
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ArcadiaSparkCompute>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    protected Stream<ArcadiaSparkCompute> loadResourcesFromAzure() {
        workspaceNode.getRemote().refresh();
        ArcadiaWorkSpace remote = workspaceNode.getRemote();
        ImmutableSortedSet<? extends IClusterDetail> clusters = remote.getClusters();
        Stream<ArcadiaSparkCompute> stream = (Stream<ArcadiaSparkCompute>) clusters.stream();
        return stream;
    }

    @NotNull
    @Override
    protected ArcadiaSparkComputeNode newResource(@NotNull ArcadiaSparkCompute arcadiaSparkCompute) {
        return new ArcadiaSparkComputeNode(arcadiaSparkCompute,this);
    }

    @NotNull
    @Override
    protected ArcadiaSparkComputeNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new ArcadiaSparkComputeNode(name,resourceGroupName,this);
    }
}

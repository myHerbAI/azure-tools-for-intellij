package com.microsoft.azure.toolkit.ide.cosmos.spark;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.AzureSparkOnCosmosService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SparkOnCosmosNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "SparkOnCosmos";
    private static final String ICON = AzureIcons.ApacheSparkOnCosmos.MODULE.getIconPath();

    @javax.annotation.Nullable
    @Override
    public Object getRoot() {
        return az(AzureSparkOnCosmosService.class);
    }
    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureSparkOnCosmosService;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureSparkOnCosmosService) {
//            final Function<AzureSynapseService, List<WorkspaceNode>> workspaces = s -> s.list().stream()
//                    .flatMap(m -> m.workspaces().list().stream()).collect(Collectors.toList());
            return new AzServiceNode<>((AzureSparkOnCosmosService) data)
                    .withIcon(ICON)
                    .withLabel("Apache Spark on Cosmos")
                    .withActions(SparkOnCosmosActionsContributor.SERVICE_ACTIONS);
//                    .addChildren(workspaces, (workspace, serviceNode) -> this.createNode(workspace, serviceNode, manager));
        } else {
            return null;
        }
    }
}

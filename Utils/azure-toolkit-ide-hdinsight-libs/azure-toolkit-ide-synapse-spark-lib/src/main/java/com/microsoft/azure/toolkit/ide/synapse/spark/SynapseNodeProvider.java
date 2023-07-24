package com.microsoft.azure.toolkit.ide.synapse.spark;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.synapse.AzureSynapseService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SynapseNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "Synapse";
    private static final String ICON = AzureIcons.ApacheSparkOnAzureSynapse.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureSynapseService.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureSynapseService;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data,@Nullable Node<?> parent,@Nonnull Manager manager) {
        if (data instanceof AzureSynapseService) {
//            final Function<AzureHDInsightService, List<SparkClusterNode>> clusters = s -> s.list().stream()
//                    .flatMap(m -> m.clusters().list().stream()).collect(Collectors.toList());
//            final Function<AzureHDInsightService, List<SparkClusterNode>> additionalClusters = AzureHDInsightService::listAdditionalCluster;
            return new AzServiceNode<>((AzureSynapseService) data)
                    .withIcon(ICON)
                    .withLabel("Synapse");
//                    .withActions(HDInsightActionsContributor.SERVICE_ACTIONS)
//                    .addChildren(additionalClusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager))
//                    .addChildren(clusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager));
        } else {
            return null;
        }
    }

}

package com.microsoft.azure.toolkit.ide.hdinsight.spark;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.hdinsight.AzureHDInsightService;
import com.microsoft.azure.toolkit.lib.hdinsight.SparkClusterNode;
import com.microsoft.azure.toolkit.lib.hdinsight.StorageAccountNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class HDInsightNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "HDInsight";
    private static final String ICON = AzureIcons.HDInsight.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureHDInsightService.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureHDInsightService
                || data instanceof SparkClusterNode
                || data instanceof StorageAccountNode;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data,@Nullable Node<?> parent,@Nonnull Manager manager) {
        if (data instanceof AzureHDInsightService) {
            final Function<AzureHDInsightService, List<SparkClusterNode>> clusters = s -> s.list().stream()
                .flatMap(m -> m.clusters().list().stream()).collect(Collectors.toList());
            final Function<AzureHDInsightService, List<SparkClusterNode>> additionalClusters = AzureHDInsightService::listAdditionalCluster;
            return new AzServiceNode<>((AzureHDInsightService) data)
                .withIcon(ICON)
                .withLabel("HDInsight")
                .withActions(HDInsightActionsContributor.SERVICE_ACTIONS)
                .addChildren(additionalClusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager))
                .addChildren(clusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager));
        } else if (data instanceof SparkClusterNode) {
            final SparkClusterNode sparkClusterNode = (SparkClusterNode) data;
            Optional.ofNullable(JobViewManager.getCluster(sparkClusterNode.getName()))
                .ifPresent(sparkClusterNode::setClusterDetail);
            final Node<SparkClusterNode> jobsNode = new AzResourceNode<>(sparkClusterNode)
                .withIcon(AzureIcon.builder().iconPath("/icons/StorageAccountFolder.png").build())
                .withDescription("")
                .onClicked(HDInsightActionsContributor.OPEN_HDINSIGHT_JOB_VIEW);
            if ("[LinkedCluster]".equals(sparkClusterNode.getClusterDetail().getSubscription().getId())) {
                return new AzResourceNode<>(sparkClusterNode)
                    .withIcon(AzureIcon.builder().iconPath("/icons/Cluster.png").build())
                    .withActions(HDInsightActionsContributor.SPARK_ADDITIONAL_CLUSTER_ACTIONS)
                    .addChild(jobsNode)
                    .addChildren(SparkClusterNode::getSubModules, (module, p) -> new AzModuleNode<>(module)
                        .withIcon(AzureIcon.builder().iconPath("/icons/StorageAccountFolder.png").build())
                        .withLabel("Stroage Accounts")
                        .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager))
                        .onClicked(HDInsightActionsContributor.OPEN_AZURE_STORAGE_EXPLORER_ON_MODULE));
            } else {
                return new AzResourceNode<>(sparkClusterNode)
                    .withIcon(AzureIcon.builder().iconPath("/icons/Cluster.png").build())
                    .withActions(HDInsightActionsContributor.SPARK_CLUSTER_ACTIONS)
                    .addChild(jobsNode)
                    .addChildren(SparkClusterNode::getSubModules, (module, p) -> new AzModuleNode<>(module)
                        .withIcon(AzureIcon.builder().iconPath("/icons/StorageAccountFolder.png").build())
                        .withLabel("Stroage Accounts")
                        .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager)));
            }
        } else if (data instanceof StorageAccountNode) {
            return new AzResourceNode<>((StorageAccountNode) data)
                .withIcon(AzureIcon.builder().iconPath("/icons/StorageAccount_16.png").build())
                .withLabel(r -> r.getRemote().name().split("\\.")[0])
                .withActions(HDInsightActionsContributor.HDINSIGHT_STORAGE_ACTIONS);
        } else {
            return null;
        }
    }

}

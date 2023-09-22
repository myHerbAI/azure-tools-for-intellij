package com.microsoft.azure.toolkit.ide.sqlserver.spark;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataModule;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SqlserverBigDataNodeProvider implements IExplorerNodeProvider {

    @Nullable
    @Override
    public Object getRoot() {
        return az(SqlserverBigDataService.class);
    }

    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof SqlserverBigDataService;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        if (data instanceof SqlserverBigDataService) {
            return new AzModuleNode<>(SqlserverBigDataModule.getInstance())
                    .withIcon(AzureIcons.SQLServerBigDataCluster.MODULE)
                    .withLabel("SQL Server Big Data Cluster")
                    .withActions(new ActionGroup(ResourceCommonActionsContributor.REFRESH));
        }
        return null;
    }

    @Override
    public boolean isAzureService() {
        return false;
    }

}

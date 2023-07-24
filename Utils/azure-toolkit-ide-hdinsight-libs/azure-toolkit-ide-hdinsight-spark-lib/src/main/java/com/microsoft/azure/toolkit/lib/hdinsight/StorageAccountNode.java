package com.microsoft.azure.toolkit.lib.hdinsight;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class StorageAccountNode extends AbstractAzResource<StorageAccountNode,SparkClusterNode, com.azure.resourcemanager.hdinsight.models.StorageAccount> {

    protected StorageAccountNode(@NotNull String name, @NotNull String resourceGroupName, @NotNull AbstractAzResourceModule<StorageAccountNode, SparkClusterNode, com.azure.resourcemanager.hdinsight.models.StorageAccount> module) {
        super(name, resourceGroupName, module);
    }

    public StorageAccountNode(@NotNull AbstractAzResource<StorageAccountNode, SparkClusterNode, com.azure.resourcemanager.hdinsight.models.StorageAccount> origin) {
        super(origin);
    }

    public StorageAccountNode(@NotNull String name, @NotNull AbstractAzResourceModule<StorageAccountNode, SparkClusterNode, com.azure.resourcemanager.hdinsight.models.StorageAccount> module) {
        super(name, module);
    }



    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    protected String loadStatus(@NotNull com.azure.resourcemanager.hdinsight.models.StorageAccount remote) {
        if(remote.isDefault())
            return "(default)";
        return StringUtils.EMPTY;
    }

}

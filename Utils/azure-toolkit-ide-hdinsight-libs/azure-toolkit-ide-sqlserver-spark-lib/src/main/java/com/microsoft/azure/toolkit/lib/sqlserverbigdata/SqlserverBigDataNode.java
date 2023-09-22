package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlserverBigDataNode extends AbstractAzResource<SqlserverBigDataNode, AzResource.None, AbstractAzResource<?, ?, ?>> {
    protected SqlserverBigDataNode(@NotNull String name, @NotNull String resourceGroupName, @NotNull AbstractAzResourceModule<SqlserverBigDataNode, None, AbstractAzResource<?, ?, ?>> module) {
        super(name, resourceGroupName, module);
    }

    @NotNull
    @Override
    protected String loadStatus(@NotNull AbstractAzResource<?, ?, ?> remote) {
        return null;
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return null;
    }
}

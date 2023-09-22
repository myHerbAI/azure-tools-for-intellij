package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.jetbrains.annotations.NotNull;

public class SqlserverBigDataService extends AbstractAzService<SqlserverBigDataServiceSubscription, SqlserverBigDataManager> {

    public SqlserverBigDataService() {
        super("Microsoft.SQLServer");
    }

    @NotNull
    @Override
    protected SqlserverBigDataServiceSubscription newResource(@NotNull SqlserverBigDataManager sqlserverBigDataManager) {
        return null;
    }
}

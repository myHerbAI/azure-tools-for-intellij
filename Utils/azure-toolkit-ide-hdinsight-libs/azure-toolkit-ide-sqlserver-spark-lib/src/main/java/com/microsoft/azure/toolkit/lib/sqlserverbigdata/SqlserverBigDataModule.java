package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.microsoft.azure.toolkit.ide.common.favorite.Favorite;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlserverBigDataModule extends AbstractAzResourceModule<SqlserverBigDataNode, AzResource.None, AbstractAzResource<?, ?, ?>> {

    public static final String NAME = "sqlserverbigdata";

    @Getter
    private static final SqlserverBigDataModule instance = new SqlserverBigDataModule();

    private SqlserverBigDataModule() {
        super(NAME, AzResource.NONE);
    }

    @NotNull
    @Override
    protected SqlserverBigDataNode newResource(@NotNull AbstractAzResource<?, ?, ?> abstractAzResource) {
        return null;
    }

    @NotNull
    @Override
    protected SqlserverBigDataNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return null;
    }

    protected boolean isAuthRequiredForListing() {
        return false;
    }

}

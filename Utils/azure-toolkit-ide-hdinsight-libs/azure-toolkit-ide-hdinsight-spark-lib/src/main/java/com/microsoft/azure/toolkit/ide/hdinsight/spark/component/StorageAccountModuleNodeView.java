package com.microsoft.azure.toolkit.ide.hdinsight.spark.component;

import com.microsoft.azure.toolkit.ide.common.component.AzureModuleLabelView;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.hdinsight.StorageAccountMudule;

import javax.annotation.Nonnull;

public class StorageAccountModuleNodeView extends AzureModuleLabelView<StorageAccountMudule> {

    private static final String name = "Storage Accounts";
    private static final String icon = "/icons/StorageAccountFolder.png";

    public StorageAccountModuleNodeView(@Nonnull AbstractAzResourceModule module){
        super((StorageAccountMudule)module,name,icon);
    }

}
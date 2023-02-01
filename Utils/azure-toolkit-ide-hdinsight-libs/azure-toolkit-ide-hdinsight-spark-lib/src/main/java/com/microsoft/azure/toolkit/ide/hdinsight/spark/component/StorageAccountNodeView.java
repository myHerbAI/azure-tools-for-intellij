package com.microsoft.azure.toolkit.ide.hdinsight.spark.component;

import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.hdinsight.StorageAccountNode;
import org.jetbrains.annotations.NotNull;

public class StorageAccountNodeView extends AzureResourceLabelView {

    private AzureIcon icon = AzureIcon.builder().iconPath("/icons/StorageAccount_16.png").build();
    private String label;
    public StorageAccountNodeView(@NotNull AzResource resource) {
        super(resource);
        StorageAccountNode storageAccountNode = (StorageAccountNode)resource;
        this.label = storageAccountNode.getRemote(true).name().split("\\.")[0];
    }

    @Override
    public String getIconPath() {
        return icon.getIconPath();
    }

    @Override
    public AzureIcon getIcon() {
        return this.icon;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

}

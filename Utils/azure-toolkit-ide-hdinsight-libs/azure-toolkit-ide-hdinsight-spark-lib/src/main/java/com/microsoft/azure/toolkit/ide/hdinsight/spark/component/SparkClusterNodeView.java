package com.microsoft.azure.toolkit.ide.hdinsight.spark.component;

import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.jetbrains.annotations.NotNull;

public class SparkClusterNodeView extends AzureResourceLabelView {

    private AzureIcon icon = AzureIcon.builder().iconPath("/icons/Cluster.png").build();

    public SparkClusterNodeView(@NotNull AzResource resource) {
        super(resource);
    }

    @Override
    public String getIconPath() {
        return icon.getIconPath();
    }

    @Override
    public AzureIcon getIcon() {
        return this.icon;
    }

}
package com.microsoft.azure.toolkit.ide.hdinsight.spark.component;

import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SparkJobNodeView extends AzureResourceLabelView {

    private String name = "Jobs";
    private  AzureIcon icon = AzureIcon.builder().iconPath("/icons/StorageAccountFolder.png").build();
    public SparkJobNodeView(@NotNull AzResource resource) {
        super(resource);
    }

    @Override
    public void setRefresher(Refresher refresher) {

    }

    @Nullable
    @Override
    public Refresher getRefresher() {
        return null;
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public String getIconPath() {
        return icon.getIconPath();
    }

    @Override
    public AzureIcon getIcon() {
        return this.icon;
    }

    @Override
    public String getDescription() {
        return StringUtils.EMPTY;
    }

    @Override
    public void dispose() {

    }

}
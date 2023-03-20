package com.microsoft.azure.toolkit.intellij.monitor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AzureMonitorToolWindowListener implements ToolWindowManagerListener {
    private boolean isTriggered = false;

    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        ToolWindowManagerListener.super.toolWindowShown(toolWindow);
        if (Objects.equals(toolWindow.getId(), AzureMonitorManager.AZURE_MONITOR_WINDOW)) {
            if (!isTriggered) {
                isTriggered = PropertiesComponent.getInstance().getBoolean(AzureMonitorManager.AZURE_MONITOR_TRIGGERED);
            }
            toolWindow.setIcon(isTriggered ? IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR) : IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR_NEW));
        }
    }
}

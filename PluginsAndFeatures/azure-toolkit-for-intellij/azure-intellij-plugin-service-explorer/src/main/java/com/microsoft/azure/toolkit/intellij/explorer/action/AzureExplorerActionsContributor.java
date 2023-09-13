package com.microsoft.azure.toolkit.intellij.explorer.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Objects;
import java.util.function.BiConsumer;

public class AzureExplorerActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> openAzureExplorer = (ignore, e) -> openAzureExplorer(e);
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER, (i, e) -> true, openAzureExplorer);
    }

    private static void openAzureExplorer(AnActionEvent e) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(Objects.requireNonNull(e.getProject())).
            getToolWindow(AzureExplorer.TOOLWINDOW_ID);
        if (Objects.nonNull(toolWindow) && !toolWindow.isVisible()) {
            AzureTaskManager.getInstance().runLater((Runnable) toolWindow::show);
        }
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }
}

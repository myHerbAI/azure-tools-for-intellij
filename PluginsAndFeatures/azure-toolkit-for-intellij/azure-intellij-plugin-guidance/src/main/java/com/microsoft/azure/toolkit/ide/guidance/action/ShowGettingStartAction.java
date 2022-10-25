package com.microsoft.azure.toolkit.ide.guidance.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.*;

public class ShowGettingStartAction extends AzureAnAction {
    private static final String GUIDANCE = "guidance";

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        if (anActionEvent.getProject() != null) {
            AzureStoreManager.getInstance().getIdeStore().setProperty(AzureConfigInitializer.NEW_FEATURE, GUIDANCE, String.valueOf(true));
            GuidanceViewManager.getInstance().showCoursesView(anActionEvent.getProject());
        }
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        final boolean ifShowRedIcon = StringUtils.isEmpty(AzureStoreManager.getInstance().getIdeStore().getProperty(AzureConfigInitializer.NEW_FEATURE, GUIDANCE));
        e.getPresentation().setIcon(IntelliJAzureIcons.getIcon(ifShowRedIcon ? GET_START_NEW : GET_START));
    }

}

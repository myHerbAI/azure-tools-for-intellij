package com.microsoft.azure.toolkit.intellij.containerapps.streaminglog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsConsoleView;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsToolWindowManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StreamingToolwindowSelectionDialog extends DialogWrapper {
    private JPanel rootPanel;
    private AzureComboBox<String> logStreamInstanceComboBox;
    private final ContainerApp containerApp;
    private final Project project;

    public StreamingToolwindowSelectionDialog(@Nullable Project project, ContainerApp containerApp) {
        super(project, false);
        this.containerApp = containerApp;
        this.project = project;
        init();
        this.logStreamInstanceComboBox.setItemsLoader(() -> StreamingLogsToolWindowManager.getInstance()
                .getResourceIdToNameMap().keySet().stream().filter(k -> k.contains(containerApp.getId()) &&
                        Optional.ofNullable(StreamingLogsToolWindowManager.getInstance().getToolWindowContent(project, k))
                                .map(StreamingLogsConsoleView::isActive).orElse(false))
                .collect(Collectors.toList()));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    @Override
    protected void doOKAction() {
        final String resourceIdToClose = logStreamInstanceComboBox.getValue();
        StreamingLogsManager.getInstance().closeStreamingLog(project, resourceIdToClose);
        super.doOKAction();
    }

    private void createUIComponents() {
        this.logStreamInstanceComboBox = new AzureComboBox<>() {
            @Override
            protected String getItemText(Object item) {
                if (Objects.isNull(item)) {
                    return StringUtils.EMPTY;
                }
                final String[] splits = item.toString().split("/");
                return splits[splits.length - 1];
            }
        };
    }
}

package com.microsoft.azure.toolkit.intellij.common.streaminglog;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StreamingLogsManager {
    private static final StreamingLogsManager instance = new StreamingLogsManager();
    public static StreamingLogsManager getInstance() {
        return instance;
    }

    public void showStreamingLog(Project project, String resourceId, String resourceName, Flux<String> logs) {
        final StreamingLogsConsoleView consoleView = Optional.ofNullable(StreamingLogsToolWindowManager.getInstance()
                .getToolWindowContent(project, resourceId)).orElse(new StreamingLogsConsoleView(project));
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, AzureString.fromString("open streaming logs"), false, () -> {
            try {
                consoleView.startStreamingLog(logs);
                AzureTaskManager.getInstance().runLater(() ->
                        StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(project, resourceId, resourceName, consoleView)
                );
            } catch (final AzureToolkitRuntimeException e) {
                throw e;
            } catch (final Throwable e) {
                throw new AzureToolkitRuntimeException("failed to open streaming log", e);
            }
        }));
    }

    public void closeStreamingLog(Project project, String resourceId) {
        final StreamingLogsConsoleView consoleView = StreamingLogsToolWindowManager.getInstance().getToolWindowContent(project, resourceId);
        if (Objects.isNull(consoleView) || !consoleView.isActive()) {
            AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager().warning("Streaming log is not started."));
            return;
        }
        consoleView.closeStreamingLog();
    }

    public boolean isStreamingLogStarted(Project project, String resourceId) {
        final List<StreamingLogsConsoleView> consoleViews = StreamingLogsToolWindowManager.getInstance().getToolWindowContents(project, resourceId);
        return consoleViews.stream().anyMatch(StreamingLogsConsoleView::isActive);
    }
}

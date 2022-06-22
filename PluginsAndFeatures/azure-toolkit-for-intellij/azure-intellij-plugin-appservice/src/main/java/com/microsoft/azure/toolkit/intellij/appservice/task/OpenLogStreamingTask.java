package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Guidance;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceTask;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.StartStreamingLogsAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;

import javax.annotation.Nonnull;

public class OpenLogStreamingTask implements GuidanceTask {
    public static final String RESOURCE_ID = "resource_id";
    private final Project project;
    private final Guidance guidance;
    private final ComponentContext context;

    public OpenLogStreamingTask(@Nonnull ComponentContext context) {
        this.context = context;
        this.project = context.getProject();
        this.guidance = context.getGuidance();
    }

    @Override
    public void execute() throws Exception {
        final String resourceId = (String) context.getParameter(RESOURCE_ID);
        final Object resource = Azure.az(AzureAppService.class).getById(resourceId);
        if (resource instanceof AppServiceAppBase) {
            new StartStreamingLogsAction((AppServiceAppBase<?, ?, ?>) resource, project).execute();
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.app_service.open_log_streaming";
    }
}

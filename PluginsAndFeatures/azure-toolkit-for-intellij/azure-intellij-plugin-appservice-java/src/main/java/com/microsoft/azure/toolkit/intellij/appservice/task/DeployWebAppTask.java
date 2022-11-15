package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Course;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeployWebAppTask implements Task {
    private final Project project;
    private final Course guidance;
    private final ComponentContext context;

    public DeployWebAppTask(@Nonnull ComponentContext context) {
        this.context = context;
        this.project = context.getProject();
        this.guidance = context.getCourse();
    }

    private RunnerAndConfigurationSettings getRunConfigurationSettings(String appId, RunManagerEx manager) {
        final ConfigurationFactory factory = WebAppConfigurationType.getInstance().getWebAppConfigurationFactory();
        final String runConfigurationName = String.format("Azure Sample: %s-%s", guidance.getName(), Utils.getTimestamp());
        final RunnerAndConfigurationSettings settings = manager.createConfiguration(runConfigurationName, factory);
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof WebAppConfiguration) {
            ((WebAppConfiguration) runConfiguration).setWebApp(Objects.requireNonNull(Azure.az(AzureWebApp.class).webApp(appId)));
            final List<AzureArtifact> allSupportedAzureArtifacts = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
            // todo: change to use artifact build by maven in last step if not exist
            final AzureArtifact azureArtifact = allSupportedAzureArtifacts.get(0);
            ((WebAppConfiguration) runConfiguration).saveArtifact(azureArtifact);
            final List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
            beforeRunTasks.add(BuildArtifactBeforeRunTaskUtils.createBuildTask(azureArtifact, runConfiguration));
            beforeRunTasks.addAll(runConfiguration.getBeforeRunTasks());
            manager.setBeforeRunTasks(runConfiguration, beforeRunTasks);
            ((WebAppConfiguration) runConfiguration).setOpenBrowserAfterDeployment(false);
        }
        return settings;
    }

    @Override
    @AzureOperation(name = "guidance.deploy_webapp", type = AzureOperation.Type.SERVICE)
    public void execute() throws Exception {
        AzureMessager.getMessager().info("Setting up run configuration for web app deployment...");
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final RunnerAndConfigurationSettings settings = getRunConfigurationSettings((String) context.getParameter(CreateWebAppTask.WEBAPP_ID), manager);
        manager.addConfiguration(settings);
        manager.setSelectedConfiguration(settings);
        final ExecutionEnvironmentBuilder executionEnvironmentBuilder = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings);
        AzureMessager.getMessager().info(AzureString.format("Executing run configuration %s...", settings.getName()));
        final ExecutionEnvironment build = executionEnvironmentBuilder.contentToReuse(null).dataContext(null).activeTarget().build();
        final SettableFuture<Void> future = SettableFuture.create();
        AzureTaskManager.getInstance().runLater(() -> ProgramRunnerUtil.executeConfigurationAsync(build, true, true, runContentDescriptor -> Objects.requireNonNull(runContentDescriptor.getProcessHandler()).addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                final Boolean result = event.getProcessHandler().getUserData(AzureRunProfileState.AZURE_RUN_STATE_RESULT);
                if (Boolean.TRUE.equals(result)) {
                    future.set(null);
                } else {
                    final Throwable throwable = event.getProcessHandler().getUserData(AzureRunProfileState.AZURE_RUN_STATE_EXCEPTION);
                    future.setException(Objects.requireNonNullElseGet(throwable, () -> new AzureToolkitRuntimeException("Execution was terminated, please see output below")));
                }
            }
        })));
        future.get();
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.webapp.deploy";
    }
}

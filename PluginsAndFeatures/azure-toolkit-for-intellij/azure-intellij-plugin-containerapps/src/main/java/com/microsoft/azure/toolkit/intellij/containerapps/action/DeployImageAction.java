package com.microsoft.azure.toolkit.intellij.containerapps.action;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper;
import com.microsoft.azure.toolkit.intellij.containerapps.AzureContainerAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeploymentType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class DeployImageAction extends AnAction {
    private static final AzureContainerAppConfigurationType configType = AzureContainerAppConfigurationType.getInstance();

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        AzureTaskManager.getInstance().runLater(() -> {
            OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
            final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
            final Project project = Objects.requireNonNull(event.getProject());
            AzureLoginHelper.requireSignedIn(project, (a) -> {
                final RunnerAndConfigurationSettings settings = getOrCreateRunConfigurationSettings(event.getProject(), null, module);
                runConfiguration(event.getProject(), settings);
            });
        });
    }

    @AzureOperation(name = "boundary/webapp.run_deploy_configuration")
    private static void runConfiguration(@Nonnull Project project, RunnerAndConfigurationSettings settings) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, message("webapp.deploy.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }

    private static RunnerAndConfigurationSettings getOrCreateRunConfigurationSettings(@Nonnull Project project, @Nullable ContainerApp app, @Nullable Module module) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getDeployImageRunConfigurationFactory();
        final String name = Optional.ofNullable(module).map(Module::getName)
                .or(() -> Optional.ofNullable(app).map(ContainerApp::getName))
                .map(n -> ":" + n)
                .orElse("");
        final String runConfigurationName = String.format("%s: %s%s", factory.getName(), project.getName(), name);
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            settings = manager.createConfiguration(runConfigurationName, factory);
        }
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof DeployImageRunConfiguration configuration) {
            Optional.ofNullable(app).map(ContainerApp::getId).ifPresent(id -> configuration.getDataModel().setContainerAppId(id));
            Optional.ofNullable(module).map(Module::getName).ifPresent(configuration.getDataModel()::setModuleName);
            configuration.setDeploymentType(DeploymentType.Code);
        }
        return settings;
    }
}

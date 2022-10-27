/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaProject;

import com.microsoft.azure.toolkit.eclipse.common.launch.AzureLongDurationTaskRunnerWithConsole;
import com.microsoft.azure.toolkit.eclipse.common.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.core.EclipseFunctionProject;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionPackager;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azuretools.core.utils.MavenUtils;

import javax.annotation.Nonnull;

public class AzureFunctionDeployLaunchDelegate extends LaunchConfigurationDelegate {
    private static final String LOCAL_SETTINGS_VALUES = "Values";

    @Override
    @AzureOperation(name = "function.deploy.configuration", type = AzureOperation.Type.ACTION)
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        final FunctionDeployConfiguration config = LaunchConfigurationUtils.getFromConfiguration(configuration,
                FunctionDeployConfiguration.class);
        final String projectName = config.getProjectName();
        final IJavaProject project = Arrays.stream(FunctionUtils.listFunctionProjects())
                .filter(t -> StringUtils.equals(t.getElementName(), projectName)).findFirst().orElse(null);
        if (project == null) {
            AzureMessager.getMessager().error("Cannot find the specified project:" + projectName);
            throw new AzureToolkitRuntimeException("Cannot find the specified project:" + projectName);
        }
        final FunctionAppConfig functionConfig = config.getFunctionConfig();
        AzureLongDurationTaskRunnerWithConsole.getInstance().runTask(AzureString
                .format("Deploy project %s to function app %s", projectName, functionConfig.getName()).toString(),
                () -> {
                    final File tempFolder = FunctionUtils.getTempStagingFolder().toFile();
                    try {
                        // build maven project
                        IFile pom = MavenUtils.getPomFile(project.getProject());
                        EclipseFunctionProject.buildMavenProject(pom);
                        // prepare staging folder
                        FileUtils.cleanDirectory(tempFolder);
                        final File file = project.getProject().getFile("host.json").getLocation().toFile();
                        final EclipseFunctionProject eclipseFunctionProject = new EclipseFunctionProject(project,
                                tempFolder);
                        eclipseFunctionProject.setHostJsonFile(file);
                        eclipseFunctionProject.buildJar();
                        AzureFunctionPackager.getInstance().packageProject(eclipseFunctionProject, true,
                                config.getFunctionCliPath());
                        FileUtils.deleteQuietly(eclipseFunctionProject.getArtifactFile());
                        // read local.settings.json and update app settings
                        if (StringUtils.isNotBlank(config.getLocalSettingsJsonPath())) {
                            updateAppSettings(new File(config.getLocalSettingsJsonPath()), functionConfig);
                        }
                        // create or update function app
                        final com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig taskConfig = FunctionAppConfig
                                .convertToTaskConfig(functionConfig);
                        final FunctionAppBase<?, ?, ?> function = new CreateOrUpdateFunctionAppTask(taskConfig).execute();
                        // deploy function app
                        new DeployFunctionAppTask(function, tempFolder, null).execute();
                        function.refresh();
                    } catch (Throwable e) {
                        AzureMessager.getMessager().error(e);
                    }
                }, false);
    }

    private static void updateAppSettings(@Nonnull final File localSettings, @Nonnull final FunctionAppConfig functionConfig) {
        if (localSettings.exists()) {
            final Map<String, String> appSettings = getAppSettingsFromLocalSettingsJson(localSettings);
            appSettings.entrySet().stream()
                    .filter(entry -> !StringUtils.isAnyEmpty(entry.getKey(), entry.getValue()))
                    .forEach(entry -> functionConfig.getAppSettings().put(entry.getKey(), entry.getValue()));
        }
    }

    private static Map<String, String> getAppSettingsFromLocalSettingsJson(@Nonnull final File target) {
        final Map<String, Object> jsonObject = JsonUtils.readFromJsonFile(target, HashMap.class);
        if (jsonObject == null) {
            return new HashMap<>();
        }
        return (Map<String, String>) jsonObject.get(LOCAL_SETTINGS_VALUES);
    }
}

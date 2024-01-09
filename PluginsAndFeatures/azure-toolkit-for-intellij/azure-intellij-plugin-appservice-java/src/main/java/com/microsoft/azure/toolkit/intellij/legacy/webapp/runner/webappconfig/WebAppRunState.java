/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.PathUtil;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.connector.IJavaAgentSupported;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DotEnvBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.webapp.*;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WebAppRunState extends AzureRunProfileState<WebAppBase<?, ?, ?>> {
    private static final String LIBS_ROOT = "/home/site/wwwroot/libs/";
    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String CATALINA_OPTS = "CATALINA_OPTS";
    private static final int DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL = 10;
    private static final int DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES = 6;
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "The app is still starting, " +
        "you could start streaming log to check if something wrong in server side.";

    private File artifact;
    private final WebAppConfiguration webAppConfiguration;
    private final IntelliJWebAppSettingModel webAppSettingModel;

    private final Map<String, String> appSettingsForResourceConnection = new HashMap<>();
    private static final String DEPLOYMENT_SUCCEED = "Deployment was successful but the app may still be starting.";

    /**
     * Place to execute the Web App deployment task.
     */
    public WebAppRunState(Project project, WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.webAppSettingModel = webAppConfiguration.getModel();
    }

    @Nullable
    @Override
    @AzureOperation(name = "user/webapp.deploy_artifact.app", params = {"this.webAppConfiguration.getWebAppName()"})
    public WebAppBase<?, ?, ?> executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        artifact = new File(getTargetPath());
        if (!artifact.exists()) {
            throw new FileNotFoundException(message("webapp.deploy.error.noTargetFile", artifact.getAbsolutePath()));
        }
        final WebAppBase<?, ?, ?> deployTarget = getOrCreateDeployTargetFromAppSettingModel(processHandler);
        // todo: migrate resource connection app settings updates to create/deploy tasks
        applyResourceConnections(deployTarget);
        updateApplicationSettings(deployTarget, processHandler);
        deployArtifactsToWebApp(deployTarget, artifact, webAppSettingModel.isDeployToRoot());
        return deployTarget;
    }

    @NotNull
    private WebAppBase<?, ?, ?> getOrCreateDeployTargetFromAppSettingModel(@NotNull RunProcessHandler processHandler) {
        final WebAppBase<?, ?, ?> result = new CreateOrUpdateWebAppTask(webAppSettingModel.getConfig()).execute();
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final AzureModule module = Optional.ofNullable(this.webAppConfiguration.getModule()).map(AzureModule::from)
                                           .or(() -> Optional.ofNullable(artifact)
                                                             .map(f -> VfsUtil.findFileByIoFile(f, true))
                                                             .map(f -> AzureModule.from(f, this.project))).orElse(null);
        if (Objects.nonNull(module)) {
            final AbstractAzResource<?, ?, ?> target = result instanceof WebAppDeploymentSlot ? result.getParent() : result;
            tm.runOnPooledThread(() -> tm.runLater(() -> tm.write(() -> module.initializeWithDefaultProfileIfNot().addApp(target).save())));
        }
        return result;
    }

    private void applyResourceConnections(@Nonnull WebAppBase<?, ?, ?> deployTarget) {
        if (webAppConfiguration.isConnectionEnabled()) {
            final DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask loadDotEnvBeforeRunTask = webAppConfiguration.getLoadDotEnvBeforeRunTask();
            loadDotEnvBeforeRunTask.loadEnv().forEach(env -> appSettingsForResourceConnection.put(env.getKey(), env.getValue()));
            webAppConfiguration.getConnections().stream()
                    .filter(connection -> connection.getResource().getDefinition() instanceof IJavaAgentSupported)
                    .forEach(connection -> uploadJavaAgent(deployTarget, ((IJavaAgentSupported) connection.getResource().getDefinition()).getJavaAgent()));
        }
    }

    private void uploadJavaAgent(@Nonnull WebAppBase<?, ?, ?> deployTarget, @Nullable File javaAgent) {
        if (javaAgent == null || !javaAgent.exists()) {
            return;
        }
        final String targetPath = PathUtil.toSystemIndependentName(Paths.get(LIBS_ROOT, javaAgent.getName()).toString());
        deployJavaAgentToAppService(deployTarget, javaAgent, targetPath);
        updateAppServiceVMOptions(deployTarget, targetPath);
    }

    private void deployJavaAgentToAppService(WebAppBase<?, ?, ?> deployTarget, File javaAgent, String targetPath) {
        AppServiceFile file;
        try {
            file = deployTarget.getFileByPath(targetPath);
        } catch (final RuntimeException e) {
            file = null;
        }
        if (file == null) {
            AzureMessager.getMessager().info(AzureString.format("Uploading java agent to web app %s", deployTarget.getName()));
            deployTarget.deploy(DeployType.STATIC, javaAgent, targetPath);
        } else {
            AzureMessager.getMessager().info(AzureString.format("Skip upload java agent as file with same name already exists"));
        }
    }

    private void updateAppServiceVMOptions(WebAppBase<?, ?, ?> deployTarget, String targetPath) {
        final Map<String, String> applicationSettings = webAppConfiguration.getApplicationSettings();
        final String containerName = Objects.requireNonNull(deployTarget.getRuntime()).getContainerName().toLowerCase();
        final String javaOptsParameter = (StringUtils.startsWithAny(containerName, "java", "jboss")) ? JAVA_OPTS : CATALINA_OPTS;
        final String javaOpts = Optional.ofNullable(webAppConfiguration.getApplicationSettings())
                .map(settings -> settings.get(javaOptsParameter)).orElse(StringUtils.EMPTY);
        final String javaAgentValue = String.format("-javaagent:%s", targetPath);
        if (StringUtils.contains(javaOpts, javaAgentValue)) {
            return;
        }
        final String value = StringUtils.isEmpty(javaOpts) ? javaAgentValue : javaOpts + " " + javaAgentValue;
        appSettingsForResourceConnection.put(javaOptsParameter, value);
    }

    private void updateApplicationSettings(WebAppBase<?, ?, ?> deployTarget, RunProcessHandler processHandler) {
        final Map<String, String> applicationSettings = new HashMap<>(ObjectUtils.firstNonNull(webAppConfiguration.getApplicationSettings(), Collections.emptyMap()));
        final Set<String> appSettingsToRemove = deployTarget.isDraft() ? Collections.emptySet() : getAppSettingsToRemove(deployTarget, applicationSettings);
        applicationSettings.putAll(appSettingsForResourceConnection);
        if (MapUtils.isEmpty(applicationSettings)) {
            return;
        }
        if (deployTarget instanceof WebApp) {
            processHandler.setText("Updating application settings...");
            final WebAppDraft draft = (WebAppDraft) deployTarget.update();
            Optional.ofNullable(appSettingsToRemove).ifPresent(keys -> keys.forEach(draft::removeAppSetting));
            draft.setAppSettings(applicationSettings);
            draft.updateIfExist();
            processHandler.setText("Update application settings successfully.");
        } else if (deployTarget instanceof WebAppDeploymentSlot) {
            processHandler.setText("Updating deployment slot application settings...");
            final WebAppDeploymentSlotDraft update = (WebAppDeploymentSlotDraft) deployTarget.update();
            update.setAppSettings(applicationSettings);
            Optional.ofNullable(appSettingsToRemove).ifPresent(keys -> keys.forEach(update::removeAppSetting));
            update.updateIfExist();
            processHandler.setText("Update deployment slot application settings successfully.");
        }
    }

    private Set<String> getAppSettingsToRemove(final WebAppBase<?, ?, ?> target, final Map<String, String> applicationSettings) {
        return Optional.ofNullable(target.getAppSettings()).map(Map::keySet).orElse(Collections.emptySet())
                .stream().filter(key -> !applicationSettings.containsKey(key))
                .collect(Collectors.toSet());
    }

    private void deployArtifactsToWebApp(final WebAppBase<?,?,?> deployTarget, final File file, final boolean isDeployToRoot) {
        final Action<Void> retry = Action.retryFromFailure(() -> deployArtifactsToWebApp(deployTarget, file, isDeployToRoot));
        final DeployType deployType = Optional.ofNullable(DeployType.fromString(FilenameUtils.getExtension(file.getName()))).orElse(DeployType.ZIP);
        final String path = isDeployToRoot || Objects.requireNonNull(deployTarget.getRuntime()).isJavaSE() ?
                            null : String.format("webapps/%s", FilenameUtils.getBaseName(file.getName()).replaceAll("#", StringUtils.EMPTY));
        final WebAppArtifact build = WebAppArtifact.builder().deployType(deployType).path(path).file(file).build();
        final DeployWebAppTask deployWebAppTask = new DeployWebAppTask(deployTarget, Collections.singletonList(build), true, false, false);
        deployWebAppTask.doExecute();
        AzureTaskManager.getInstance().runInBackground("get deployment status", () -> {
            OperationContext.current().setMessager(AzureMessager.getDefaultMessager());
            if (!deployWebAppTask.waitUntilDeploymentReady(false, DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL, DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES)) {
                AzureMessager.getMessager().warning(GET_DEPLOYMENT_STATUS_TIMEOUT, null,
                                                    AzureActionManager.getInstance().getAction(AppServiceActionsContributor.START_STREAM_LOG).bind(deployTarget));
            } else {
                AzureMessager.getMessager().success(AzureString.format("App({0}) started successfully.", deployTarget.getName()), null,
                                                    AzureActionManager.getInstance().getAction(AppServiceActionsContributor.OPEN_IN_BROWSER).bind(deployTarget));
            }
        });
    }

    private void openWebAppInBrowser(String url, RunProcessHandler processHandler) {
        try {
            Desktop.getDesktop().browse(new URL(url).toURI());
        } catch (final IOException | URISyntaxException e) {
            processHandler.println(e.getMessage(), ProcessOutputTypes.STDERR);
        }
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP);
    }

    @Override
    protected void onSuccess(WebAppBase<?, ?, ?> result, @NotNull RunProcessHandler processHandler) {
        final String fileName = FileNameUtils.getBaseName(artifact.getName());
        final String fileType = FileNameUtils.getExtension(artifact.getName());
        final String url = getUrl(result, fileName, fileType);
        processHandler.setText(DEPLOYMENT_SUCCEED);
        processHandler.setText("URL: " + url);
        if (webAppSettingModel.isOpenBrowserAfterDeployment()) {
            openWebAppInBrowser(url, processHandler);
        }
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("artifactType", webAppConfiguration.getAzureArtifactType() == null ? null : webAppConfiguration.getAzureArtifactType().name());
        properties.putAll(webAppSettingModel.getTelemetryProperties(Collections.EMPTY_MAP));
        return properties;
    }

    @AzureOperation(name = "internal/webapp.get_artifact.app", params = {"this.webAppConfiguration.getName()"})
    private String getTargetPath() {
        final AzureArtifact azureArtifact =
                AzureArtifactManager.getInstance(project).getAzureArtifactById(webAppConfiguration.getAzureArtifactType(),
                                                                               webAppConfiguration.getArtifactIdentifier());
        if (Objects.isNull(azureArtifact)) {
            final String error = String.format("selected artifact[%s] not found", webAppConfiguration.getArtifactIdentifier());
            throw new AzureToolkitRuntimeException(error);
        }
        return azureArtifact.getFileForDeployment();
    }

    @NotNull
    private String getUrl(@NotNull WebAppBase<?, ?, ?> webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.getHostName();
        if (Objects.equals(fileType, MavenConstants.TYPE_WAR) && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + WebAppUtils.encodeURL(fileName.replaceAll("#", StringUtils.EMPTY)).replaceAll("\\+", "%20");
        }
        return url;
    }
}

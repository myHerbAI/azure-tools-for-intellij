/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.base;

import com.azure.core.implementation.http.HttpClientProviders;
import com.azure.core.management.AzureEnvironment;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.util.EnvironmentUtil;
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.DefaultMachineStore;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore;
import com.microsoft.azure.toolkit.intellij.common.task.IntellijAzureTaskManager;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.DeprecatedWebAppOnLinuxDeployConfigurationFactory;
import com.microsoft.azure.toolkit.intellij.network.NetworkDiagnose;
import com.microsoft.azure.toolkit.intellij.network.ProxyUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.core.mvp.ui.base.AppSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.PluginSettings;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.FileHandler;

import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY;
import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY_INSTALLATION_ID;
import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY_PLUGIN_VERSION;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.OPERATION_NAME;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.SERVICE_NAME;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_INSTALL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_LOAD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_UPGRADE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

@Slf4j
public class PluginLifecycleListener implements AppLifecycleListener {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static final FileHandler logFileHandler = null;

    private PluginSettings settings;

    static {
        // fix the class load problem for intellij plugin
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(PluginLifecycleListener.class.getClassLoader());
            HttpClientProviders.createInstance();
            Azure.az(AzureAccount.class);
            Hooks.onErrorDropped(ex -> {
                final Throwable cause = ExceptionUtils.getRootCause(ex);
                if (cause instanceof InterruptedException) {
                    log.info(ex.getMessage());
                } else if (cause instanceof UnknownHostException) {
                    NetworkDiagnose.checkAzure(AzureEnvironment.AZURE).publishOn(Schedulers.parallel()).subscribe(sites -> {
                        final Map<String, String> properties = new HashMap<>();
                        properties.put(SERVICE_NAME, SYSTEM);
                        properties.put(OPERATION_NAME, "network_diagnose");
                        properties.put("sites", sites);
                        properties.put(PROXY, Boolean.toString(StringUtils.isNotBlank(Azure.az().config().getProxySource())));
                        AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
                    });
                } else {
                    throw Lombok.sneakyThrow(ex);
                }
            });
        } catch (final Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/common.init_plugin")
    public void appFrameCreated(@Nonnull List<String> commandLineArgs) {
        try {
            AzureTaskManager.register(new IntellijAzureTaskManager());
            AzureRxTaskManager.register();
            final String azureJson = String.format("%s%s%s", PluginUtil.getPluginRootDirectory(), File.separator, "azure.json");
            AzureStoreManager.register(new DefaultMachineStore(azureJson),
                IntellijStore.getInstance(), IntelliJSecureStore.getInstance());
            ProxyUtils.initProxy();
            initializeConfig();
            initializeTelemetry();
            AzureMessager.setDefaultMessager(new IntellijAzureMessager());
            IntellijAzureActionManager.register();
            SchedulerProviderFactory.getInstance().init(new AppSchedulerProvider());
            CommandUtils.setEnv(EnvironmentUtil.getEnvironmentMap());
            // workaround fixes for web app on linux run configuration
            AzureDockerSupportConfigurationType.registerConfigurationFactory("Web App for Containers", DeprecatedWebAppOnLinuxDeployConfigurationFactory::new);
            final ActionManager am = ActionManager.getInstance();
            final DefaultActionGroup toolbarGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_MAIN_TOOLBAR);
            toolbarGroup.addAll((DefaultActionGroup) am.getAction("AzureToolbarGroup"));
            final DefaultActionGroup popupGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
            popupGroup.add(am.getAction("AzurePopupGroup"));
            IdeAzureAccount.getInstance().restoreSignin(); // restore sign in
        } catch (final Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    private static void initializeTelemetry() {
        final String oldVersion = AzureStoreManager.getInstance().getIdeStore().getProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION);
        if (StringUtils.isBlank(oldVersion)) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
        } else if (StringUtils.isNotBlank(oldVersion) && !com.microsoft.azure.toolkit.intellij.common.CommonConst.PLUGIN_VERSION.equalsIgnoreCase(oldVersion)) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Load, null, true);
        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_LOAD, null, null);
        if (StringUtils.isNotBlank(Azure.az().config().getProxySource())) {
            final Map<String, String> map = Optional.ofNullable(AzureTelemeter.getCommonProperties()).map(HashMap::new).orElse(new HashMap<>());
            map.put(PROXY, "true");
            AzureTelemeter.setCommonProperties(map);
        }
    }

    public static void initializeConfig() {
        String installId = AzureStoreManager.getInstance().getIdeStore().getProperty(null, TELEMETRY_INSTALLATION_ID);

        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.getHashMac();
        }
        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get());
        }

        AzureConfigInitializer.initialize(installId, "Azure Toolkit for IntelliJ", CommonConst.PLUGIN_VERSION);
        CommonSettings.setUserAgent(Azure.az().config().getUserAgent());
        if (StringUtils.isNotBlank(Azure.az().config().getCloud())) {
            Azure.az(AzureCloud.class).setByName(Azure.az().config().getCloud());
        }
    }
}

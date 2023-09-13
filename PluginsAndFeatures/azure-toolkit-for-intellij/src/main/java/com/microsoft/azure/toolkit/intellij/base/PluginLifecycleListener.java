/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.base;

import com.azure.core.implementation.http.HttpClientProviders;
import com.azure.core.management.AzureEnvironment;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
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
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

@Slf4j
public class PluginLifecycleListener implements AppLifecycleListener, PluginStateListener {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static final FileHandler logFileHandler = null;

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
                    PluginLifecycleListener.checkAzure(AzureEnvironment.AZURE).publishOn(Schedulers.parallel()).subscribe(sites -> {
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
            final String azureJson = String.format("%s%s%s", CommonConst.PLUGIN_PATH, File.separator, "azure.json");
            AzureStoreManager.register(new DefaultMachineStore(azureJson),
                IntellijStore.getInstance(), IntelliJSecureStore.getInstance());
            initProxy();
            initializeConfig();
            initializeTelemetry();
            AzureMessager.setDefaultMessager(new IntellijAzureMessager());
            IntellijAzureActionManager.register();
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
        final String newVersion = CommonConst.PLUGIN_VERSION;
        if (StringUtils.isBlank(oldVersion)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.install-plugin.version", newVersion));
        } else if (StringUtils.isNotBlank(oldVersion) && !newVersion.equalsIgnoreCase(oldVersion)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.upgrade-plugin.from|to", oldVersion, newVersion));
        }
        AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.load-plugin.version", newVersion));
        if (StringUtils.isNotBlank(Azure.az().config().getProxySource())) {
            final Map<String, String> map = Optional.ofNullable(AzureTelemeter.getCommonProperties()).map(HashMap::new).orElse(new HashMap<>());
            map.put(PROXY, "true");
            AzureTelemeter.setCommonProperties(map);
        }
    }

    private static void initializeConfig() {
        String installId = AzureStoreManager.getInstance().getIdeStore().getProperty(null, TELEMETRY_INSTALLATION_ID);

        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.getHashMac();
        }
        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get());
        }

        AzureConfigInitializer.initialize(installId, "Azure Toolkit for IntelliJ", CommonConst.PLUGIN_VERSION);
        if (StringUtils.isNotBlank(Azure.az().config().getCloud())) {
            Azure.az(AzureCloud.class).setByName(Azure.az().config().getCloud());
        }
    }

    private static Mono<String> checkAzure(AzureEnvironment env) {
        final List<String> urls = new ArrayList<>();
        urls.add("https://www.microsoft.com");
        urls.add(env.getActiveDirectoryEndpoint());
        urls.add(env.getManagementEndpoint());

        return Flux.fromIterable(urls).map(PluginLifecycleListener::getDomainName)
            .map(PluginLifecycleListener::isHostAvailable).filter(StringUtils::isNotBlank)
            .onErrorContinue((throwable, o) -> {
                System.out.println("Cannot check host for:" + o);
            })
            .collectList().map(res -> StringUtils.join(res, ";"));
    }

    @SneakyThrows
    private static String getDomainName(String url) {
        final URI uri = new URI(url);
        return uri.getHost();
    }

    private static String isHostAvailable(String hostName) {
        try (final Socket socket = new Socket()) {
            final InetSocketAddress socketAddress = new InetSocketAddress(hostName, 443);
            socket.connect(socketAddress, 2000);
            return hostName;
        } catch (final IOException e) {
            return StringUtils.EMPTY;
        }
    }

    private static void initProxy() {
        final HttpConfigurable instance = HttpConfigurable.getInstance();
        if (instance != null && instance.USE_HTTP_PROXY) {
            final ProxyInfo proxy = ProxyInfo.builder()
                .source("intellij")
                .host(instance.PROXY_HOST)
                .port(instance.PROXY_PORT)
                .username(instance.getProxyLogin())
                .password(instance.getPlainProxyPassword())
                .build();
            Azure.az().config().setProxyInfo(proxy);
            ProxyManager.getInstance().applyProxy();
        }
        setSslContext();
    }

    private static void setSslContext() {
        final CertificateManager certificateManager = CertificateManager.getInstance();
        Azure.az().config().setSslContext(certificateManager.getSslContext());
        HttpsURLConnection.setDefaultSSLSocketFactory(certificateManager.getSslContext().getSocketFactory());
    }

    @Override
    public void install(@Nonnull IdeaPluginDescriptor ideaPluginDescriptor) {
        if (ideaPluginDescriptor.getPluginId().getIdString().equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.install-plugin.version", CommonConst.PLUGIN_VERSION));
        }
    }

    @Override
    public void uninstall(@Nonnull IdeaPluginDescriptor ideaPluginDescriptor) {
        if (ideaPluginDescriptor.getPluginId().getIdString().equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.uninstall-plugin.version", CommonConst.PLUGIN_VERSION));
        }
    }
}

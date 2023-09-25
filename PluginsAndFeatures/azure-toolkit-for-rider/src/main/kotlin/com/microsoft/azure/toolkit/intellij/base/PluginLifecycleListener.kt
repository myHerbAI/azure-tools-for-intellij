package com.microsoft.azure.toolkit.intellij.base

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.net.ssl.CertificateManager
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer
import com.microsoft.azure.toolkit.intellij.common.CommonConst
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureCloud
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils
import javax.net.ssl.HttpsURLConnection

class PluginLifecycleListener : AppLifecycleListener {
    companion object {
        private val LOG = logger<PluginLifecycleListener>()
    }

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        try {
            AzureRxTaskManager.register()
            initProxy()
            initializeConfig()
            IntellijAzureActionManager.register()
            IdeAzureAccount.getInstance().restoreSignin()
        } catch (t: Throwable) {
            LOG.error(t)
        }
    }

    private fun initializeConfig() {
        var installId = InstallationIdUtils.getHashMac()
        if (installId.isNullOrBlank() || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get())
        }

        AzureConfigInitializer.initialize(installId, "Azure Toolkit for IntelliJ", CommonConst.PLUGIN_VERSION)
        val cloud = Azure.az().config().cloud
        if (cloud.isNotBlank()) {
            Azure.az(AzureCloud::class.java).setByName(cloud)
        }
    }

    private fun initProxy() {
        val instance = HttpConfigurable.getInstance()
        if (instance != null && instance.USE_HTTP_PROXY) {
            val proxy = ProxyInfo.builder()
                    .source("intellij")
                    .host(instance.PROXY_HOST)
                    .port(instance.PROXY_PORT)
                    .username(instance.proxyLogin)
                    .password(instance.plainProxyPassword)
                    .build()
            Azure.az().config().setProxyInfo(proxy)
            ProxyManager.getInstance().applyProxy()
        }
        val certificateManager = CertificateManager.getInstance()
        Azure.az().config().sslContext = certificateManager.sslContext
        HttpsURLConnection.setDefaultSSLSocketFactory(certificateManager.sslContext.socketFactory)
    }
}
package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.docker.DockerCloudConfiguration
import com.intellij.docker.DockerCloudType
import com.intellij.openapi.project.Project
import com.intellij.remoteServer.impl.configuration.deployment.RemoteServerComboWithAutoDetect
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import javax.swing.JPanel

class WebAppContainersSettingPanel(private val project: Project, configuration: WebAppContainersConfiguration) : RiderAzureSettingPanel<WebAppContainersConfiguration>() {
    private val panel: JPanel

    private lateinit var dockerServerComboBox: Cell<RemoteServerComboWithAutoDetect<DockerCloudConfiguration>>
    private lateinit var webAppContainersComboBox: Cell<WebAppContainersComboBox>
    private lateinit var portSpinner: Cell<JBIntSpinner>

    init {
        val serverType = DockerCloudType.getInstance()

        panel = panel {
            row("Docker Server:") {
                dockerServerComboBox = cell(RemoteServerComboWithAutoDetect(serverType))
                        .align(Align.FILL)
            }
            row("Web App:") {
                webAppContainersComboBox = dockerWebAppComboBox(project)
                        .align(Align.FILL)
            }
            row("Website Port:") {
                portSpinner = spinner(80..65535)
            }
        }

        webAppContainersComboBox.component.reloadItems()
    }

    override fun apply(configuration: WebAppContainersConfiguration) {
        val webappConfig = webAppContainersComboBox.component.value
        webappConfig?.let { configuration.setWebAppConfig(webappConfig) }
        configuration.port = portSpinner.component.number
    }

    override fun reset(configuration: WebAppContainersConfiguration) {
        if (configuration.webAppId.isNullOrEmpty() && configuration.webAppName.isNullOrEmpty()) return
        portSpinner.component.number = configuration.port
    }

    override fun disposeEditor() {
    }

    override fun getMainPanel() = panel
}
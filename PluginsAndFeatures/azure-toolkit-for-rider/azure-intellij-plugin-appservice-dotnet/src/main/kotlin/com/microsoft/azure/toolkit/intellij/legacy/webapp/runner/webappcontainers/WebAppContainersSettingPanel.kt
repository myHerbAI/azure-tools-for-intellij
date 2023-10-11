package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.docker.DockerCloudType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.GrowPolicy
import com.microsoft.azure.toolkit.intellij.common.*
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import javax.swing.JLabel
import javax.swing.JPanel

class WebAppContainersSettingPanel(private val project: Project) : RiderAzureSettingPanel<WebAppContainersConfiguration>() {
    private val panel: JPanel

//    private lateinit var dockerServerComboBox: Cell<RemoteServerComboWithAutoDetect<DockerCloudConfiguration>>
//    private lateinit var dockerImageComboBox: Cell<AzureDockerImageComboBox>
    private lateinit var containerRegistryComboBox: Cell<AzureContainerRegistryComboBox>
    private lateinit var repositoryLabel: Cell<JLabel>
    private lateinit var repositoryTextField: Cell<JBTextField>
    private lateinit var tagTextField: Cell<JBTextField>
    private lateinit var webAppContainersComboBox: Cell<WebAppContainersComboBox>
    private lateinit var portSpinner: Cell<JBIntSpinner>

    init {
        val serverType = DockerCloudType.getInstance()

        panel = panel {
//            row("Docker Server:") {
//                dockerServerComboBox = cell(RemoteServerComboWithAutoDetect(serverType))
//                        .align(Align.FILL)
//            }
//            row("Image:") {
//                dockerImageComboBox = dockerImageComboBox(project)
//                        .align(Align.FILL)
//            }
            row("Container Registry:") {
                containerRegistryComboBox = dockerContainerRegistryComboBox()
                        .align(Align.FILL)
            }
            row("Repository:") {
                repositoryLabel = label("")
                repositoryTextField = textField()
            }
            row("Tag:") {
                tagTextField = textField()
                        .columns(COLUMNS_TINY)
            }
            row("Web App:") {
                webAppContainersComboBox = dockerWebAppComboBox(project)
                        .align(Align.FILL)
            }
            row("Website Port:") {
                portSpinner = spinner(80..65535)
            }
        }

        tagTextField.component.text = "latest"

//        dockerServerComboBox.component.addChangeListener(::onServerChanged)
        containerRegistryComboBox.component.addValueChangedListener(::onRegistryChanged)

        webAppContainersComboBox.component.reloadItems()
    }

    override fun apply(configuration: WebAppContainersConfiguration) {
        val webappConfig = webAppContainersComboBox.component.value
        webappConfig?.let { configuration.setWebAppConfig(it) }
        val dockerPushConfiguration = getDockerConfiguration()
        dockerPushConfiguration.let { configuration.setDockerPushConfiguration(it) }
        configuration.port = portSpinner.component.number
    }

    private fun getDockerConfiguration(): DockerPushConfiguration {
//        val dockerServer = dockerServerComboBox.component.selectedServer?.name
//        val dockerImage = dockerImageComboBox.component.value
        val registry = containerRegistryComboBox.component.value
        val repository = repositoryTextField.component.text
        val tag = tagTextField.component.text

        return DockerPushConfiguration(
                registryAddress = registry?.address,
                repositoryName = repository,
                tagName = tag,
                null,
                null
        )
    }

    override fun reset(configuration: WebAppContainersConfiguration) {
        if (configuration.webAppId.isNullOrEmpty() && configuration.webAppName.isNullOrEmpty()) return
        webAppContainersComboBox.component.value = configuration.getWebAppConfig()
        setDockerConfiguration(configuration.getDockerPushConfiguration())
        portSpinner.component.number = configuration.port
    }

    private fun setDockerConfiguration(configuration: DockerPushConfiguration) {
//        dockerServerComboBox.component.selectServerInCombo(configuration.dockerServer)
//        dockerImageComboBox.component.value = configuration.dockerImage
        configuration.registryAddress?.let { containerRegistryComboBox.component.setRegistry(it) }
        repositoryTextField.component.text = configuration.repositoryName
        tagTextField.component.text = configuration.tagName
    }

//    private fun onServerChanged(e: ChangeEvent) {
//        val selected = dockerServerComboBox.component.selectedServer
//        if (selected == null || selected.configuration.customConfiguratorId != "DockerDefaultConnectionConfigurator")
//            dockerImageComboBox.component.setDockerServer(null)
//        else
//            dockerImageComboBox.component.setDockerServer(selected)
//    }

    private fun onRegistryChanged(value: ContainerRegistryModel) {
        repositoryLabel.component.text = "${value.address}/"
    }

    override fun disposeEditor() {
    }

    override fun getMainPanel() = panel
}
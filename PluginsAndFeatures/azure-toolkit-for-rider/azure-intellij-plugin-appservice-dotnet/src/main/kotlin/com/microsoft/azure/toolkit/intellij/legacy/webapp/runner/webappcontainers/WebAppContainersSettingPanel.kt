/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.microsoft.azure.toolkit.intellij.common.*
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import javax.swing.JLabel
import javax.swing.JPanel

class WebAppContainersSettingPanel(private val project: Project) : RiderAzureSettingPanel<WebAppContainersConfiguration>() {
    private val panel: JPanel

    private lateinit var containerRegistryComboBox: Cell<AzureContainerRegistryComboBox>
    private lateinit var repositoryLabel: Cell<JLabel>
    private lateinit var repositoryTextField: Cell<JBTextField>
    private lateinit var tagTextField: Cell<JBTextField>
    private lateinit var webAppContainersComboBox: Cell<WebAppContainersComboBox>
    private lateinit var portSpinner: Cell<JBIntSpinner>

    init {
        panel = panel {
            row("Container Registry:") {
                containerRegistryComboBox = dockerContainerRegistryComboBox()
                        .align(Align.FILL)
            }
            row("Repository:") {
                repositoryLabel = label("")
                repositoryTextField = textField()
                label("Tag:")
                tagTextField = textField().columns(COLUMNS_TINY)
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
        configuration.registryAddress?.let { containerRegistryComboBox.component.setRegistry(it) }
        repositoryTextField.component.text = configuration.repositoryName
        tagTextField.component.text = configuration.tagName
    }

    private fun onRegistryChanged(value: ContainerRegistryModel) {
        repositoryLabel.component.text = "${value.address}/"
    }

    override fun getMainPanel() = panel

    override fun disposeEditor() {
    }
}
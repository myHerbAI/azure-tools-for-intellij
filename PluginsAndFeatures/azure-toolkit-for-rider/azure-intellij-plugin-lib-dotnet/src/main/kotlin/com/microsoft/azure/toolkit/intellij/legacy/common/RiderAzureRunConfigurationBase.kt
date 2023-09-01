package com.microsoft.azure.toolkit.intellij.legacy.common

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

abstract class RiderAzureRunConfigurationBase<T : Any>(project: Project, factory: ConfigurationFactory, name: String?) :
        LocatableConfigurationBase<T>(project, factory, name) {
    abstract fun getModel(): T

    abstract fun validate()

    override fun readExternal(element: Element) {
        super.readExternal(element)
        XmlSerializer.deserializeInto(getModel(), element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        XmlSerializer.serializeInto(getModel(), element)
    }
}
/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.utils

import com.intellij.json.psi.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.containers.ContainerUtil

object JsonParser {
    fun jsonFileFromVirtualFile(virtualFile: VirtualFile, project: Project): JsonFile? {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null
        val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null
        return file as? JsonFile
    }

    fun writeProperty(
        project: Project,
        jsonObject: JsonObject,
        propertyName: String,
        propertyValue: String,
        escapeValue: Boolean = true
    ): JsonProperty {
        val generator = JsonElementGenerator(project)
        val property =
            if (escapeValue) {
                generator.createProperty(propertyName, "\"" + StringUtil.escapeStringCharacters(propertyValue) + "\"")
            } else {
                generator.createProperty(propertyName, propertyValue)
            }

        val existingProperty = jsonObject.findProperty(propertyName)
        if (existingProperty != null) {
            existingProperty.replace(property)
            return property
        }

        val list = jsonObject.propertyList
        return if (list.isEmpty()) {
            jsonObject.addAfter(property, jsonObject.firstChild)
            property
        } else {
            val comma = jsonObject.addAfter(generator.createComma(), ContainerUtil.getLastItem(list))
            jsonObject.addAfter(property, comma) as JsonProperty
        }
    }

    fun findStringProperty(jsonObject: JsonObject?, propertyName: String): String? =
        findProperty<JsonStringLiteral>(jsonObject, propertyName)?.value

    fun findBooleanProperty(jsonObject: JsonObject?, propertyName: String): Boolean? =
        findProperty<JsonBooleanLiteral>(jsonObject, propertyName)?.value

    fun findNumberProperty(jsonObject: JsonObject?, propertyName: String): Number? =
        findProperty<JsonNumberLiteral>(jsonObject, propertyName)?.value

    private inline fun <reified T> findProperty(jsonObject: JsonObject?, propertyName: String): T? {
        val property = jsonObject?.findProperty(propertyName) ?: return null
        return property.value as? T
    }
}
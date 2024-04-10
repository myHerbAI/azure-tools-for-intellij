/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.storage

import com.intellij.icons.AllIcons
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.jvm.java
import kotlin.let

class StorageAccountComboBox : AzureComboBox<StorageAccountConfig>() {
    private var subscriptionId: String? = null
    private var resourceGroupName: String? = null
    private val drafts = mutableListOf<StorageAccountConfig>()

    fun setSubscription(newSubscription: String?) {
        if (newSubscription == subscriptionId) return

        subscriptionId = newSubscription
        if (subscriptionId == null) {
            clear()
        } else {
            reloadItems()
        }
    }

    fun setResourceGroup(newResourceGroup: String?) {
        if (newResourceGroup == resourceGroupName) return
        resourceGroupName = newResourceGroup
    }

    override fun setValue(value: StorageAccountConfig?, fixed: Boolean?) {
        if (value != null && isDraftResource(value)) {
            drafts.remove(value)
            drafts.add(0, value)
            reloadItems()
        }

        super.setValue(value, fixed)
    }

    override fun getItemText(item: Any?): String? {
        if (item == null || item !is StorageAccountConfig) return EMPTY_ITEM

        val name = item.name
        return if (isDraftResource(item)) "(New) $name" else name
    }

    override fun loadItems(): List<StorageAccountConfig> {
        val sid = subscriptionId ?: return emptyList()

        val result = mutableListOf<StorageAccountConfig>()

        if (!drafts.isEmpty()) {
            drafts
                .asSequence()
                .filter { it.subscriptionId == sid && it.resourceGroupName == resourceGroupName }
                .sortedBy { it.name }
                .forEach { result.add(it) }
        }

        var remoteAccounts = Azure.az(AzureStorageAccount::class.java)
            .accounts(sid)
            .list()
            .asSequence()
            .sortedBy { it.name }

        resourceGroupName?.let { rgName ->
            remoteAccounts = remoteAccounts.filter { it.resourceGroupName.equals(rgName, true) }
        }

        result.addAll(
            remoteAccounts
                .map { StorageAccountConfig(it.subscriptionId, it.resourceGroupName, it.name) }
                .toList()
        )

        return result
    }

    override fun refreshItems() {
        subscriptionId?.let {
            Azure.az(AzureStorageAccount::class.java).accounts(it).refresh()
        }
        super.refreshItems()
    }

    override fun getExtensions(): List<Extension?> {
        val extensions = super.getExtensions() as MutableList<Extension?>

        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK)
        val tooltip = "Create new storage account ${KeymapUtil.getKeystrokeText(keyStroke)}"
        val addEx = Extension.create(AllIcons.General.Add, tooltip, ::showStorageAccountCreationPopup)
        registerShortcut(keyStroke, addEx)
        extensions.add(addEx)

        return extensions
    }

    private fun showStorageAccountCreationPopup() {
        val sid = subscriptionId ?: return
        val dialog = StorageAccountCreationDialog(sid, resourceGroupName)
        val actionId = Action.Id.of<StorageAccountConfig>("user/storage.create_account.group")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(StorageAccountConfig::name)
                .withSource { it }
                .withAuthRequired(false)
                .withHandler { setValue(it) }
        )
        dialog.show()
    }

    private fun isDraftResource(value: StorageAccountConfig) =
        Azure.az(AzureStorageAccount::class.java)
            .accounts(value.subscriptionId)
            .exists(value.name, value.resourceGroupName)
            .not()
}

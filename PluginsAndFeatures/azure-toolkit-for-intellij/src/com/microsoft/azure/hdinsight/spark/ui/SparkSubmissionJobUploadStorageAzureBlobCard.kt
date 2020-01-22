/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.hdinsight.spark.common.getSecureStoreServiceOf
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import com.microsoft.intellij.forms.dsl.panel
import org.apache.commons.lang3.StringUtils
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel
import javax.swing.JTextField

class SparkSubmissionJobUploadStorageAzureBlobCard: SparkSubmissionJobUploadStorageBasicCard() {
    interface Model : SparkSubmissionJobUploadStorageBasicCard.Model {
        var storageAccount : String?
        var storageKey : String?
        var containersModel : ComboBoxModel<Any>
        var selectedContainer: String?
    }

    private val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)
    private val refreshButtonIconPath = "/icons/refresh.png"
    private val storageAccountTip = "The default storage account of the HDInsight cluster, which can be found from HDInsight cluster properties of Azure portal."
    private val storageKeyTip = "The storage key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal."
    private val storageAccountLabel = JLabel("Storage Account").apply { toolTipText = storageAccountTip }
    val storageAccountField = JTextField().apply { toolTipText = storageAccountTip; name = "blobCardStorageAccountField" }
    private val storageKeyLabel = JLabel("Storage Key").apply { toolTipText = storageKeyTip }
    val storageKeyField = ExpandableTextField().apply { toolTipText = storageKeyTip; name = "blobCardStorageKeyField" }
    private val storageContainerLabel = JLabel("Storage Container")
    val storageContainerUI = ComboboxWithBrowseButton().apply {
        comboBox.name = "blobCardStorageContainerComboBoxCombo"
        button.name = "blobCardStorageContainerComboBoxButton"
        button.toolTipText = "Refresh"
        button.icon = StreamUtil.getImageResourceFile(refreshButtonIconPath)
    }

    init {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = ANCHOR_WEST
                }
                col {
                    anchor = ANCHOR_WEST
                    hSizePolicy = SIZEPOLICY_WANT_GROW
                    fill = FILL_HORIZONTAL
                }
            }
            row {
                c(storageAccountLabel.apply { labelFor = storageAccountField });    c(storageAccountField)
            }
            row {
                c(storageKeyLabel.apply { labelFor = storageKeyField });            c(storageKeyField)
            }
            row {
                c(storageContainerLabel.apply { labelFor = storageContainerUI });   c(storageContainerUI)
            }
        }

        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }

    override val title = SparkSubmitStorageType.BLOB.description
    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        to.storageAccount = storageAccountField.text?.trim()
        to.storageKey = storageKeyField.text?.trim()
        to.containersModel = storageContainerUI.comboBox.model
        to.selectedContainer = storageContainerUI.comboBox.selectedItem?.toString()
    }

    override fun writeWithLock(from: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (from !is Model) {
            return
        }

        if (storageAccountField.text != from.storageAccount) {
            storageAccountField.text = from.storageAccount
        }

        val credentialAccount = SparkSubmitStorageType.BLOB.getSecureStoreServiceOf(from.storageAccount)
        val storageKeyToSet =
                if (StringUtils.isBlank(from.errorMsg) && StringUtils.isEmpty(from.storageKey)) {
                    credentialAccount?.let { secureStore?.loadPassword(credentialAccount, from.storageAccount) }
                } else {
                    from.storageKey
                }

        if (storageKeyField.text != storageKeyToSet) {
            storageKeyField.text = storageKeyToSet
        }

        if (storageContainerUI.comboBox.model != from.containersModel) {
            if (from.containersModel.size == 0
                    && StringUtils.isNotEmpty(from.selectedContainer)) {
                storageContainerUI.comboBox.model = DefaultComboBoxModel(arrayOf(from.selectedContainer))
            } else {
                storageContainerUI.comboBox.model = from.containersModel
            }
        }
    }
}
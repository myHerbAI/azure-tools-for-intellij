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

package com.microsoft.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.hdinsight.common.CommonConst
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction
import com.microsoft.tooling.msservices.components.DefaultLoader

class DisableSslCertificateValidationAction : AzureAnAction(), Toggleable {
    private val warningPrompt: String =
            """
By clicking on the 'Disable' option, the IntelliJ Azure Toolkit will bypass Spark cluster 
security certificate verification when connecting to Spark clusters. Validation for the 
following certificate issues(but not limited to) will be skipped.

  1) The security certificate is from an untrusted certifying authority.
  2) The security certificate is expired.
  3) The security certificate is invalid.
  4) The name on the security certificate is invalid or does not match the name of the site.
            """.trimIndent()

    override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
        try {
            if (!isActionEnabled()) {
                val messageResult = Messages.showYesNoDialog(
                        anActionEvent.project,
                        warningPrompt,
                        "Disable SSL Certificate Verification",
                        "Proceed",
                        "Cancel",
                        UIUtil.getWarningIcon()
                )

                if (messageResult == Messages.YES) {
                    anActionEvent.presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, !isActionEnabled())
                    DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.DISABLE_SSL_CERTIFICATE_VALIDATION, (!isActionEnabled()).toString())
                }
            } else {
                anActionEvent.presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, !isActionEnabled())
                DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.DISABLE_SSL_CERTIFICATE_VALIDATION, (!isActionEnabled()).toString())
            }
        } catch (ignored: RuntimeException) {
        }

        return true
    }

    companion object {
        @JvmStatic
        fun isActionEnabled(): Boolean {
            return DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.DISABLE_SSL_CERTIFICATE_VALIDATION)
                    && DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.DISABLE_SSL_CERTIFICATE_VALIDATION).toBoolean()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, isActionEnabled())
    }
}

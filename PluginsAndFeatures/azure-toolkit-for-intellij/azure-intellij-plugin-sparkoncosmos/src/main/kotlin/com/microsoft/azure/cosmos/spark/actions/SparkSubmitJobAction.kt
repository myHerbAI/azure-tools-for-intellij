package com.microsoft.azure.cosmos.spark.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation

open class SeqActions(private vararg val actionIds: String): AzureAnAction(), ILogger {
    override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
        try {
            for (actiondId: String in actionIds) {
                val action = ActionManagerEx.getInstanceEx().getAction(actiondId)
                action?.actionPerformed(anActionEvent)
                    ?: log().error("Can't perform the action with id $actiondId")
            }
        } catch (ignored: RuntimeException) {
        }

        return true
    }
}

class CosmosServerlessSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosServerlessSparkType", "Actions.SubmitSparkApplicationAction")

class CosmosSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosSparkType", "Actions.SubmitSparkApplicationAction")
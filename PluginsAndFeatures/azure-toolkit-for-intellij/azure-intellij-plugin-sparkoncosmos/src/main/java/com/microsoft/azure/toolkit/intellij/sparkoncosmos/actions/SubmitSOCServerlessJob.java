package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkProvisionDialog;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.spark.actions.SparkAppSubmitContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;

import java.util.List;

public class SubmitSOCServerlessJob {
    public static void submit(SparkOnCosmosADLAccountNode target, AnActionEvent e) {

    }
}

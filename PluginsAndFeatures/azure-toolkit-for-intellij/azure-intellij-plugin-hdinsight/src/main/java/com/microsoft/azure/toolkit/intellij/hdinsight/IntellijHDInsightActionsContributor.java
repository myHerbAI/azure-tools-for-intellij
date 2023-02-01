package com.microsoft.azure.toolkit.intellij.hdinsight;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.HDInsightActionsContributor;
import com.microsoft.azure.toolkit.intellij.hdinsight.actions.OpenAzureStorageFormAction;
import com.microsoft.azure.toolkit.intellij.hdinsight.actions.OpenLinkAClusterAction;
import com.microsoft.azure.toolkit.intellij.hdinsight.actions.OpenSparkJobViewAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.hdinsight.SparkClusterNode;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijHDInsightActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzResource, AnActionEvent> jobviewCondition = (r, e) -> r instanceof SparkClusterNode;
        final BiConsumer<AzResource, AnActionEvent> jobviewHandler = (c, e) -> OpenSparkJobViewAction.open((SparkClusterNode) c, e);
        am.registerHandler(HDInsightActionsContributor.OPEN_HDINSIGHT_JOB_VIEW, jobviewCondition, jobviewHandler);

        final BiPredicate<Object, AnActionEvent> linkClusterCondition = (r, e) -> r instanceof Object;
        final BiConsumer<Object, AnActionEvent> linkClusterHandler = (c, e) -> OpenLinkAClusterAction.open(c, e);
        am.registerHandler(HDInsightActionsContributor.LINK_A_CLUSTER, linkClusterCondition, linkClusterHandler);

        final BiPredicate<AzResourceModule, AnActionEvent> openAzureStorageForm = (r, e) -> r instanceof Object;
        final BiConsumer<AzResourceModule, AnActionEvent> azureStorageFormHandler = (c, e) -> OpenAzureStorageFormAction.open(c, e);
        am.registerHandler(HDInsightActionsContributor.OPEN_AZURE_STORAGE_EXPLORER_ON_MODULE, openAzureStorageForm, azureStorageFormHandler);


    }
}
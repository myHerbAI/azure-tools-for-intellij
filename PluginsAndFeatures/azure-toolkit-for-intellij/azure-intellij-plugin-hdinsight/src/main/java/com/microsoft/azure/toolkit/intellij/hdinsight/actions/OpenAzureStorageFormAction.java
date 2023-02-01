package com.microsoft.azure.toolkit.intellij.hdinsight.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.ClusterNode;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.StorageAccountFolderNode;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.ui.WarningMessageForm;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class OpenAzureStorageFormAction {

    public static void open(Object target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            createRefreshHdiReaderStorageAccountsWarningForm(
                        e.getProject(), ClusterNode.ASE_DEEP_LINK);
        });
    }

    public static void createRefreshHdiReaderStorageAccountsWarningForm(Project project,
                                                                 @NotNull final String aseDeepLink) {
        AzureTaskManager.getInstance().runLater(new Runnable() {
            @Override
            public void run() {
                final String title = "Storage Access Denied";
                final String warningText = "<html><pre>"
                        + "You have Read-only permission for this cluster. Please ask the cluster owner or <br>"
                        + "user access administrator to upgrade your role to HDInsight Cluster Operator in the "
                        + "Azure Portal, or <br>use 'Open Azure Storage Explorer' to access the storages "
                        + "associated with this cluster."
                        + "</pre></html>";
                final String okButtonText = "Open Azure Storage Explorer";
                final WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        try {
                            DefaultLoader.getIdeHelper().openLinkInBrowser(aseDeepLink);
                        } catch (final Exception ex) {
                            DefaultLoader.getUIHelper().showError(ex.getMessage(), "HDInsight Explorer");
                        }

                    }
                };
                form.show();
            }
        }, AzureTask.Modality.ANY);
    }

}

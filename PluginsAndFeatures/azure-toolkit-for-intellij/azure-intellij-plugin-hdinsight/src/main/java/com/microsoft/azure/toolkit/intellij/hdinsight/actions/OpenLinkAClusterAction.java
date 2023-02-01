package com.microsoft.azure.toolkit.intellij.hdinsight.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewClusterForm;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.hdinsight.AzureHDInsightService;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class OpenLinkAClusterAction {

    public static void open(Object target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            int originalSize = ClusterManagerEx.getInstance().getAdditionalClusterDetails().size();
            AddNewClusterForm addNewClusterForm = new AddNewClusterForm(e.getProject(), null);
            addNewClusterForm.show();
            int changedSize = ClusterManagerEx.getInstance().getAdditionalClusterDetails().size();
            if(originalSize!=changedSize){
                AzureHDInsightService service = az(AzureHDInsightService.class);
                service.refresh();
            }
        });
    }

}

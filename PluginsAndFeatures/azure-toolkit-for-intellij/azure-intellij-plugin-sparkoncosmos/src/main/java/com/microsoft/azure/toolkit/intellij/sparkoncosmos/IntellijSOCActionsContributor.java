package com.microsoft.azure.toolkit.intellij.sparkoncosmos;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.cosmos.spark.SparkOnCosmosActionsContributor;
import com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions.ProvisionClusterAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijSOCActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {

        final BiPredicate<Object, AnActionEvent> provisionClusterCondition = (r, e) -> r instanceof Object;
        final BiConsumer<Object, AnActionEvent> provisionClusterHandler = (c, e) -> ProvisionClusterAction.provision((SparkOnCosmosADLAccountNode)c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.PROVISION_CLUSTER, provisionClusterCondition, provisionClusterHandler);

    }
}

package com.microsoft.azure.toolkit.lib.synapse.models;

import com.azure.resourcemanager.synapse.models.Workspace;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;

public interface WorkspaceDetail extends Workspace {
    ArcadiaWorkSpace workspace = null;
}

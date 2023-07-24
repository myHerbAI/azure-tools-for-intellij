/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.util.messages.Topic;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

public interface DeploymentTargetTopics {

    Topic<TargetAppChanged> TARGET_APP_CHANGED = Topic.create("connector.targetApp.changed", TargetAppChanged.class);

    interface TargetAppChanged {
        void appChanged(AzureModule module, AbstractAzResource<?, ?, ?> app, Action change);
    }

    enum Action {
        ADD, REMOVE
    }
}

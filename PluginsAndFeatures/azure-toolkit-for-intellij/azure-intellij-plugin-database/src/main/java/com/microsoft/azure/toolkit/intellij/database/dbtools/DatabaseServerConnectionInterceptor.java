/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.database.dataSource.DatabaseConnectionInterceptor;
import com.intellij.database.dataSource.DatabaseConnectionPoint;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.OPERATION_NAME;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.OP_NAME;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.OP_TYPE;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.SERVICE_NAME;

@SuppressWarnings("UnstableApiUsage")
public class DatabaseServerConnectionInterceptor implements DatabaseConnectionInterceptor {
    @Nullable
    public CompletionStage<ProtoConnection> intercept(@Nonnull DatabaseConnectionInterceptor.ProtoConnection proto, boolean silent) {
        final DatabaseConnectionPoint point = proto.getConnectionPoint();
        final String accountId = point.getAdditionalProperty(DatabaseServerParamEditor.KEY_DB_SERVER_ID);
        if (StringUtils.isNotBlank(accountId) && !StringUtils.equals(accountId, DatabaseServerParamEditor.NONE)) {
            final Map<String, String> properties = new HashMap<>();
            final ResourceId id = ResourceId.fromString(accountId);
            properties.put("subscriptionId", id.subscriptionId());
            properties.put(SERVICE_NAME, "database");
            properties.put(OPERATION_NAME, "connect_jdbc_from_dbtools");
            properties.put(OP_NAME, "database.connect_jdbc_from_dbtools");
            properties.put(OP_TYPE, AzureOperation.Type.ACTION.name());
            AzureTelemeter.log(AzureTelemetry.Type.OP_END, properties);
        }
        return null;
    }
}
/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.connection;

import com.microsoft.azure.toolkit.intellij.database.connection.SpringSqlDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlDatabase;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlDatabase;

import java.util.List;

public class JavaSqlServerDatabaseResourceDefinition extends SqlServerDatabaseResourceDefinition implements SpringSqlDatabaseResourceDefinition<MicrosoftSqlDatabase> {
    @Override
    public List<MicrosoftSqlDatabase> getResources() {
        return Azure.az(AzureSqlServer.class).servers().stream()
                .flatMap(s -> s.databases().list().stream())
                .collect(java.util.stream.Collectors.toList());
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.connection;

import com.microsoft.azure.toolkit.intellij.database.connection.SpringSqlDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlDatabase;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlDatabase;

import java.util.List;

public class JavaPostgreSqlDatabaseResourceDefinition extends PostgreSqlDatabaseResourceDefinition implements SpringSqlDatabaseResourceDefinition<PostgreSqlDatabase> {
    @Override
    public List<PostgreSqlDatabase> getResources() {
        return Azure.az(AzurePostgreSql.class).servers().stream()
                .flatMap(s -> s.databases().list().stream())
                .collect(java.util.stream.Collectors.toList());
    }
}

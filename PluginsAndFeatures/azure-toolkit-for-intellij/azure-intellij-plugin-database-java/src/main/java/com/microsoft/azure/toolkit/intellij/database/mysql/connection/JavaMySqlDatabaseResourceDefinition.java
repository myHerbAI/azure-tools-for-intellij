/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.mysql.connection;

import com.microsoft.azure.toolkit.intellij.database.connection.SpringSqlDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlDatabase;

import java.util.List;

public class JavaMySqlDatabaseResourceDefinition extends MySqlDatabaseResourceDefinition implements SpringSqlDatabaseResourceDefinition<MySqlDatabase> {
    @Override
    public List<MySqlDatabase> getResources() {
        return Azure.az(AzureMySql.class).servers().stream()
                .flatMap(s -> s.databases().list().stream())
                .collect(java.util.stream.Collectors.toList());
    }
}

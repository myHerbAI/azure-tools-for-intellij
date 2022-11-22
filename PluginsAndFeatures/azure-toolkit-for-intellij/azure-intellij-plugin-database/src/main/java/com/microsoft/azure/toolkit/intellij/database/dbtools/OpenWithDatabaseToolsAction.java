/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.Dbms;
import com.intellij.database.autoconfig.DataSourceDetector;
import com.intellij.database.autoconfig.DataSourceRegistry;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.view.ui.DataSourceManagerDialog;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;

public class OpenWithDatabaseToolsAction {
    @AzureOperation(name = "database.open_database_tools.server", params = {"server.getName()"}, type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    public static void openDataSourceManagerDialog(IDatabaseServer<?> server, Project project) {
        final DataSourceRegistry registry = new DataSourceRegistry(project);
        final DbPsiFacade dbPsiFacade = DbPsiFacade.getInstance(project);
        final DataSourceDetector.Builder builder = registry.getBuilder()
            .withDriverClass(server.getJdbcUrl().getDefaultDriverClass())
            .withUrl(server.getJdbcUrl().toString())
            .withDbms(getDbms(server))
            .withJdbcAdditionalProperty(DatabaseServerParamEditor.KEY_DB_SERVER_ID, server.getId())
            .commit();
        DataSourceManagerDialog.showDialog(dbPsiFacade, registry);
    }

    private static Dbms getDbms(IDatabaseServer<?> server) {
        if (MicrosoftSqlServer.class.equals(server.getClass())) {
            return Dbms.MSSQL;
        } else if (MySqlServer.class.equals(server.getClass())) {
            return Dbms.MYSQL;
        } else if (PostgreSqlServer.class.equals(server.getClass())) {
            return Dbms.POSTGRES;
        }
        return Dbms.MYSQL;
    }
}

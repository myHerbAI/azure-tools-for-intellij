/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.dataSource.url.TypeDescriptor;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class DatabaseServerTypeFactory implements TypesRegistry.TypeDescriptorFactory {

    public static final String MYSQL = "az_mysql_server";
    public static final String SQLSERVER = "az_sqlserver_server";
    public static final String POSTGRE = "az_postgre_server";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypeDescriptor> consumer) {
        consumer.consume(TypesRegistry.createTypeDescriptor(MYSQL, ".", "Server"));
        consumer.consume(TypesRegistry.createTypeDescriptor(SQLSERVER, ".", "Server"));
        consumer.consume(TypesRegistry.createTypeDescriptor(POSTGRE, ".", "Server"));
    }
}

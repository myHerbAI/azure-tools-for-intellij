/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

intellij {
    plugins = listOf("com.intellij.database")
}

dependencies {
    compileOnly(project(path = ":azure-intellij-plugin-lib"))
    runtimeOnly(project(path = ":azure-intellij-plugin-lib", configuration = "instrumentedJar"))
    compileOnly(project(path = ":azure-intellij-plugin-database"))
    runtimeOnly(project(path = ":azure-intellij-plugin-database", configuration = "instrumentedJar"))
    compileOnly(project(path = ":azure-intellij-resource-connector-lib"))
    runtimeOnly(project(path = ":azure-intellij-resource-connector-lib", configuration = "instrumentedJar"))
    implementation("com.microsoft.azure:azure-toolkit-database-lib")
    implementation("com.microsoft.azure:azure-toolkit-mysql-lib")
    implementation("com.microsoft.azure:azure-toolkit-sqlserver-lib")
    implementation("com.microsoft.azure:azure-toolkit-postgre-lib")
}
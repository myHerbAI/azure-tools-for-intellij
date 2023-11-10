/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

dependencies {
    implementation("com.microsoft.azure:azure-toolkit-auth-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    compileOnly(project(path = ":azure-intellij-plugin-lib"))
    runtimeOnly(project(path = ":azure-intellij-plugin-lib", configuration = "instrumentedJar"))
    compileOnly(project(path = ":azure-intellij-plugin-lib-dotnet"))
    runtimeOnly(project(path = ":azure-intellij-plugin-lib-dotnet", configuration = "instrumentedJar"))
    compileOnly(project(path = ":azure-intellij-plugin-appservice"))
    runtimeOnly(project(path = ":azure-intellij-plugin-appservice", configuration = "instrumentedJar"))
    compileOnly(project(path = ":azure-intellij-resource-connector-lib"))
    runtimeOnly(project(path = ":azure-intellij-resource-connector-lib", configuration = "instrumentedJar"))
    implementation("com.microsoft.azure:azure-toolkit-appservice-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-appservice-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerregistry-lib")
    implementation("io.ktor:ktor-client-core:2.3.6")
    implementation("io.ktor:ktor-client-cio:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
}
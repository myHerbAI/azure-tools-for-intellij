plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}

repositories {
    mavenCentral()
    mavenLocal()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

val platformVersion: String by extra
val azureToolkitVersion: String by extra

dependencies {
    intellijPlatform {
        rider(platformVersion)
        jetbrainsRuntime()
        bundledPlugins(listOf("com.jetbrains.restClient"))
        instrumentationTools()
    }

    implementation("com.microsoft.azure:azure-toolkit-auth-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-lib-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-appservice"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-resharper-host"))
    implementation("com.microsoft.azure:azure-toolkit-appservice-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-appservice-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerregistry-lib:$azureToolkitVersion")
}

tasks {
    buildPlugin { enabled = false }
    runIde { enabled = false }
    prepareSandbox { enabled = false }
    buildSearchableOptions { enabled = false }
    patchPluginXml { enabled = false }
    publishPlugin { enabled = false }
    verifyPlugin { enabled = false }
}

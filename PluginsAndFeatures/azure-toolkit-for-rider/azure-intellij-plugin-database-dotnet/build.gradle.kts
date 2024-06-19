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
        bundledPlugins(listOf("com.intellij.database"))
        instrumentationTools()
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-database"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation("com.microsoft.azure:azure-toolkit-database-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-mysql-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-sqlserver-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-postgre-lib:$azureToolkitVersion")
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
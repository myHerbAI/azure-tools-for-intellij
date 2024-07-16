plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.intellij.platform.module")
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
        rider(platformVersion, false)
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

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
        instrumentationTools()
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-redis"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation("com.microsoft.azure:azure-toolkit-redis-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-redis-lib:$azureToolkitVersion")
}

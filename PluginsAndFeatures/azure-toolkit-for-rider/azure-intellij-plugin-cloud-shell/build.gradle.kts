plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
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
        rider(platformVersion)
        jetbrainsRuntime()
        bundledPlugins(listOf("org.jetbrains.plugins.terminal"))
        instrumentationTools()
    }

    implementation("org.java-websocket:Java-WebSocket:1.5.1")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}

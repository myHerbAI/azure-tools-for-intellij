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
        rider(platformVersion)
        jetbrainsRuntime()
        bundledPlugins(listOf("org.jetbrains.plugins.terminal"))
        instrumentationTools()
    }

    implementation("org.java-websocket:Java-WebSocket:1.5.1")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")
}

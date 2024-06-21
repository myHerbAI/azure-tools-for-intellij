plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.1")
}

tasks {
    buildPlugin { enabled = false }
    patchPluginXml { enabled = false }
    prepareSandbox { enabled = false }
    publishPlugin { enabled = false }
    runIde { enabled = false }
    signPlugin { enabled = false }
    buildSearchableOptions { enabled = false }
    verifyPlugin { enabled = false }
}
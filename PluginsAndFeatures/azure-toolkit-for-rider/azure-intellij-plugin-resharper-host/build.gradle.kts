import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

val platformVersion: String by extra

dependencies {
    intellijPlatform {
        rider(platformVersion)
        jetbrainsRuntime()
        instrumentationTools()
    }
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
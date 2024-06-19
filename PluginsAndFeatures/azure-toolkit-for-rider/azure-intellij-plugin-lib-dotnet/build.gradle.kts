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
        bundledPlugins(listOf("Docker"))
        instrumentationTools()
    }

    implementation("com.microsoft.azure:azure-toolkit-auth-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")
    implementation(project(path = ":azure-intellij-plugin-lib"))
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
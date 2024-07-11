plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.aspectj)
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
        bundledPlugins(listOf("com.intellij.properties", "org.jetbrains.plugins.terminal"))
        instrumentationTools()
    }

    implementation("com.microsoft.azure:azure-toolkit-libs:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-libs:$azureToolkitVersion")
    implementation("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-libs:0.1.1")

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-guidance"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-monitor"))
    implementation("com.microsoft.azure:azure-toolkit-appservice-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-appservice-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerregistry-lib:$azureToolkitVersion")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.codehaus.plexus:plexus-archiver:4.2.7")
    implementation("org.codehaus.plexus:plexus-container-default:2.1.1")
    implementation("com.neovisionaries:nv-websocket-client:2.14")

    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.jetbrains:annotations:24.0.0")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("com.microsoft.azure:azure-toolkit-common-lib:$azureToolkitVersion")
    aspect("com.microsoft.azure:azure-toolkit-common-lib:$azureToolkitVersion")
}

configurations {
    implementation { exclude(module = "slf4j-api") }
    implementation { exclude(module = "log4j") }
    implementation { exclude(module = "stax-api") }
    implementation { exclude(module = "groovy-xml") }
    implementation { exclude(module = "groovy-templates") }
    implementation { exclude(module = "jna") }
    implementation { exclude(module = "xpp3") }
    implementation { exclude(module = "pull-parser") }
    implementation { exclude(module = "xsdlib") }
}

tasks {
    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}

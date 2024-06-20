/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import com.jetbrains.plugin.structure.base.utils.isFile
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

val azureToolkitVersion by extra { properties("azureToolkitVersion").get() }
val platformVersion by extra { properties("platformVersion").get() }

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    mavenLocal()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(platform("com.microsoft.azure:azure-toolkit-libs:${azureToolkitVersion}"))
    implementation(platform("com.microsoft.azure:azure-toolkit-ide-libs:${azureToolkitVersion}"))
    implementation(platform("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-libs:0.1.1"))

    implementation("com.microsoft.azure:azure-toolkit-common-lib:${azureToolkitVersion}")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-lib-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-guidance"))
    implementation(project(path = ":azure-intellij-plugin-arm"))
    implementation(project(path = ":azure-intellij-plugin-monitor"))
    implementation(project(path = ":azure-intellij-plugin-appservice"))
    implementation(project(path = ":azure-intellij-plugin-appservice-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-database"))
    implementation(project(path = ":azure-intellij-plugin-database-dotnet"))

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        rider(platformVersion)

        jetbrainsRuntime()

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(properties("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Bundled)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map {
            listOf(
                it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    verifyPlugin {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    val rdGen = ":protocol:rdgen"

    val dotnetBuildConfiguration = properties("dotnetBuildConfiguration").get()
    val compileDotNet by registering {
        dependsOn(rdGen)
        doLast {
            exec {
                executable("dotnet")
                args("build", "-c", dotnetBuildConfiguration, "/clp:ErrorsOnly", "ReSharper.Azure.sln")
            }
        }
    }

    withType<KotlinCompile> {
        dependsOn(rdGen)
    }

    buildPlugin {
        dependsOn(compileDotNet)
    }

    withType<PrepareSandboxTask> {
        dependsOn(compileDotNet)

        val outputFolder = file("$projectDir/src/dotnet/ReSharper.Azure")

        val dllFiles = listOf(
            "$outputFolder/Azure.Project/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Project.dll",
            "$outputFolder/Azure.Project/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Project.pdb",
            "$outputFolder/Azure.Psi/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Psi.dll",
            "$outputFolder/Azure.Psi/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Psi.pdb",
            "$outputFolder/Azure.Intellisense/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Intellisense.dll",
            "$outputFolder/Azure.Intellisense/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Intellisense.pdb",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Daemon.dll",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Daemon.pdb",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/NCrontab.Signed.dll",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/CronExpressionDescriptor.dll"
        )

        for (f in dllFiles) {
            from(f) { into("${rootProject.name}/dotnet") }
        }

        doLast {
            for (f in dllFiles) {
                val file = file(f)
                if (!file.exists()) throw RuntimeException("File \"$file\" does not exist")
            }
        }
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

val riderModel: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(riderModel.name, provider {
        intellijPlatform.platformPath.resolve("lib/rd/rider-model.jar").also {
            check(it.isFile) {
                "rider-model.jar is not found at $riderModel"
            }
        }
    }) {
        builtBy(Constants.Tasks.INITIALIZE_INTELLIJ_PLATFORM_PLUGIN)
    }
}

//// Configure project's dependencies
//repositories {
//    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
//    intellijPlatform {
//        defaultRepositories()
//        jetbrainsRuntime()
//    }
//}

//// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
//intellij {
//    pluginName = properties("pluginName")
//    version = properties("platformVersion")
//    type = properties("platformType")
//
//    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
//    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
//}

//// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
//changelog {
//    groups.empty()
//    repositoryUrl = properties("pluginRepositoryUrl")
//}

//sourceSets {
//    main {
//        kotlin.srcDir("src/main/kotlin")
//        resources.srcDir("src/main/resources")
//    }
//}

//val azureToolkitVersion = properties("azureToolkitVersion").get()

//allprojects {
//    apply {
//        plugin("org.jetbrains.kotlin.jvm")
//        plugin("org.jetbrains.kotlin.plugin.serialization")
//        plugin("org.jetbrains.intellij.platform")
//        plugin("io.freefair.aspectj.post-compile-weaving")
//        plugin("io.spring.dependency-management")
//    }

//    tasks {
//        compileJava {
//            sourceCompatibility = "17"
//            targetCompatibility = "17"
//        }
//
//        processResources {
//            duplicatesStrategy = DuplicatesStrategy.WARN
//        }
//    }

//    repositories {
//        mavenCentral()
//        mavenLocal()
//        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
//        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
//    }

//    dependencyManagement {
//        imports {
//            mavenBom("com.microsoft.azure:azure-toolkit-libs:$azureToolkitVersion")
//            mavenBom("com.microsoft.azure:azure-toolkit-ide-libs:$azureToolkitVersion")
//        }
//    }

//    dependencies {
//        compileOnly("org.projectlombok:lombok")
//        annotationProcessor("org.projectlombok:lombok")
//        implementation("com.microsoft.azure:azure-toolkit-common-lib")
//        aspect("com.microsoft.azure:azure-toolkit-common-lib")
//        compileOnly("org.jetbrains:annotations")
//        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

//        // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
//        intellijPlatform {
//            rider(properties("platformVersion"))
//
//            jetbrainsRuntime()
//
//            // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
//            bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })
//
//            // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
//            plugins(properties("platformPlugins").map { it.split(',') })
//
//            instrumentationTools()
//            pluginVerifier()
//            testFramework(TestFrameworkType.Bundled)
//        }
//    }

//    configurations {
//        implementation { exclude(module = "slf4j-api") }
//        implementation { exclude(module = "log4j") }
//        implementation { exclude(module = "stax-api") }
//        implementation { exclude(module = "groovy-xml") }
//        implementation { exclude(module = "groovy-templates") }
//        implementation { exclude(module = "jna") }
//        implementation { exclude(module = "xpp3") }
//        implementation { exclude(module = "pull-parser") }
//        implementation { exclude(module = "xsdlib") }
//    }

//    intellij {
//        version = properties("platformVersion")
//        type = properties("platformType")
//
//        plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
//    }
//}


//dependencies {
//    implementation(project(path = ":azure-intellij-plugin-lib-dotnet", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-resource-connector-lib", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-plugin-service-explorer", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-plugin-guidance", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-plugin-appservice-dotnet", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-plugin-arm", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-plugin-monitor", configuration = "instrumentedJar"))
//    implementation(project(path = ":azure-intellij-plugin-database-dotnet", configuration = "instrumentedJar"))
//}

//subprojects {
//    tasks {
//        buildPlugin { enabled = false }
//        runIde { enabled = false }
//        prepareSandbox { enabled = false }
////        prepareTestingSandbox { enabled = false }
//        buildSearchableOptions { enabled = false }
//        patchPluginXml { enabled = false }
//        publishPlugin { enabled = false }
//        verifyPlugin { enabled = false }
//    }
//}


//val rdLibDirectory: () -> File = { file("${tasks.setupDependencies.get().idea.get().classes}/lib/rd") }
//extra["rdLibDirectory"] = rdLibDirectory


//    patchPluginXml {
//        version = properties("pluginVersion")
//        sinceBuild = properties("pluginSinceBuild")
//        untilBuild = properties("pluginUntilBuild")
//
//        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
//        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
//            val start = "<!-- Plugin description -->"
//            val end = "<!-- Plugin description end -->"
//
//            with(it.lines()) {
//                if (!containsAll(listOf(start, end))) {
//                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
//                }
//                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
//            }
//        }
//
//        val changelog = project.changelog // local variable for configuration cache compatibility
//        // Get the latest available change notes from the changelog file
//        changeNotes = properties("pluginVersion").map { pluginVersion ->
//            with(changelog) {
//                renderItem(
//                    (getOrNull(pluginVersion) ?: getUnreleased())
//                        .withHeader(false)
//                        .withEmptySections(false),
//                    Changelog.OutputType.HTML,
//                )
//            }
//        }
//    }

//    runPluginVerifier {
//        ideVersions.set(
//            properties("pluginVerifierIdeVersions").get().split(',').map(String::trim).filter(String::isNotEmpty)
//        )
//    }

//    configure<com.jetbrains.rd.generator.gradle.RdGenExtension> {
//        val modelDir = projectDir.resolve("protocol").resolve("src").resolve("main")
//            .resolve("kotlin").resolve("model").resolve("daemon")
//        val csGeneratedOutput = projectDir.resolve("src").resolve("dotnet").resolve("ReSharper.Azure")
//            .resolve("Azure.Daemon").resolve("Protocol")
//        val ktGeneratedOutput = projectDir.resolve("azure-intellij-plugin-resharper-host").resolve("src")
//            .resolve("main").resolve("kotlin").resolve("org").resolve("jetbrains").resolve("protocol")
//
//        verbose = true
//        hashFolder = "build/rdgen"
//        packages = "model.daemon"
//        classpath({
//            logger.info("Calculating classpath for rdgen, intellij.ideaDependency is ${rdLibDirectory().canonicalPath}")
//            rdLibDirectory().resolve("rider-model.jar").canonicalPath
//        })
//        sources(modelDir)
//
//        generator {
//            language = "kotlin"
//            transform = "asis"
//            root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
//            namespace = "com.jetbrains.rider.azure.model"
//            directory = ktGeneratedOutput.canonicalPath
//        }
//
//        generator {
//            language = "csharp"
//            transform = "reversed"
//            root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
//            namespace = "JetBrains.Rider.Azure.Model"
//            directory = csGeneratedOutput.canonicalPath
//        }
//    }

//    signPlugin {
//        certificateChain = environment("CERTIFICATE_CHAIN")
//        privateKey = environment("PRIVATE_KEY")
//        password = environment("PRIVATE_KEY_PASSWORD")
//    }

//    publishPlugin {
//        dependsOn("patchChangelog")
//        token = environment("PUBLISH_TOKEN")
//        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
//        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
//        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
//        channels = properties("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
//    }


plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("com.jetbrains.rdgen") version "2022.1.2"
    id("org.jetbrains.intellij") version "1.7.0"
    id("me.filippov.gradle.jvm.wrapper") version "0.11.0"
}

group = "com.jetbrains"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

apply {
    plugin("kotlin")
}

intellij {
    version.set("2022.2-SNAPSHOT")
    type.set("RD")
    instrumentCode.set(false)
}

val resharperPluginPath = projectDir.resolve("ReSharper.Azure")
val rdLibDirectory: () -> File = { file("${tasks.setupDependencies.get().idea.get().classes}/lib/rd") }
extra["rdLibDirectory"] = rdLibDirectory

tasks {
    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("")
    }

    runIde {
        maxHeapSize = "8g"
    }

    rdgen {
        verbose = true
        hashFolder = "build/rdgen"
        logger.info("Configuring rdgen params")
        classpath({
            logger.info("Calculating classpath for rdgen, intellij.ideaDependency is ${rdLibDirectory().canonicalPath}")
            rdLibDirectory().resolve("rider-model.jar").canonicalPath
        })
        val resharperPluginPath = projectDir.resolve("ReSharper.Azure")
        val csDaemonGeneratedOutput = resharperPluginPath.resolve("src")
            .resolve("Azure.Daemon").resolve("Protocol")
        val ktGeneratedOutput = projectDir.resolve("src").resolve("main").resolve("kotlin")
            .resolve("org").resolve("jetbrains").resolve("protocol")
        sources(projectDir.resolve("protocol").resolve("src")
            .resolve("main").resolve("kotlin"))
        packages = "model.daemon"

        generator {
            language = "kotlin"
            transform = "asis"
            root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
            namespace = "com.jetbrains.rider.azure.model"
            directory = ktGeneratedOutput.canonicalPath
        }

        generator {
            language = "csharp"
            transform = "reversed"
            root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
            namespace = "JetBrains.Rider.Azure.Model"
            directory = csDaemonGeneratedOutput.canonicalPath
        }
    }

    compileJava {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        dependsOn(rdgen)
    }
}


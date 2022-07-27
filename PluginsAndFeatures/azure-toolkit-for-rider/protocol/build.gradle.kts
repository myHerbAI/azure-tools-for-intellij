plugins {
    id("org.jetbrains.kotlin.jvm")
}

val rdLibDirectory: () -> File by rootProject.extra

repositories {
    flatDir {
        dir(rdLibDirectory())
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(group = "", name = "rd-gen")
    implementation(group = "", name = "rider-model")
}
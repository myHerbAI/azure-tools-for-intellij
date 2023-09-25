dependencies {
    compileOnly(project(path = ":azure-intellij-plugin-lib"))
    runtimeOnly(project(path = ":azure-intellij-plugin-lib", configuration = "instrumentedJar"))
}
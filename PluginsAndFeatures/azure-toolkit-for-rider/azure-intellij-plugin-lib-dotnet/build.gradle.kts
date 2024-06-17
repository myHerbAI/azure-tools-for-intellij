dependencies {
    implementation("com.microsoft.azure:azure-toolkit-auth-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    compileOnly(project(path = ":azure-intellij-plugin-lib"))
    runtimeOnly(project(path = ":azure-intellij-plugin-lib", configuration = "instrumentedJar"))
}
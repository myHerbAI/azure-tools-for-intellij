dependencies {
    compileOnly(project(path = ":azure-intellij-plugin-lib-dotnet"))
    runtimeOnly(project(path = ":azure-intellij-plugin-lib-dotnet", configuration = "instrumentedJar"))
    compileOnly(project(path = ":azure-intellij-plugin-appservice"))
    runtimeOnly(project(path = ":azure-intellij-plugin-appservice", configuration = "instrumentedJar"))
}
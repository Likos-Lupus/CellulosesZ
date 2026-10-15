plugins {
    id("dev.kikugie.stonecutter")
    id("command-catalog")
    id("translations")
    id("help-copy")
    id("architecture")
    id("module-dependencies")
    id("artifact-inspection")
    id("compat")
}

stonecutter active "26.1.2-fabric" /* [SC] DO NOT EDIT */

tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds every registered loader cell."
    dependsOn("verifyArchitecture", "checkModuleDependencies")
    dependsOn(stonecutter.tasks.named("build").map { it.values })
}

tasks.register("chiseledClean") {
    group = "build"
    description = "Cleans every registered loader cell."
    dependsOn(stonecutter.tasks.named("clean").map { it.values })
}

tasks.register("buildActive") {
    group = "build"
    description = "Builds the active loader cell."
    dependsOn("verifyArchitecture", "checkModuleDependencies")
    dependsOn(":${stonecutter.current?.project ?: error("No active Stonecutter version")}:build")
}

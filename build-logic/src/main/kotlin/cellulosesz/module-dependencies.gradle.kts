// Project-dependency allowlist. Any other feature -> feature edge fails the build.
val dependencyAllowlist: Map<String, Set<String>> = mapOf(
    ":modules:foundation" to emptySet(),
    ":modules:minecraft-core" to setOf(":modules:foundation"),
    ":modules:movement" to setOf(":modules:foundation", ":modules:minecraft-core"),
    ":modules:communication" to setOf(":modules:foundation", ":modules:minecraft-core"),
    ":modules:administration" to setOf(":modules:foundation", ":modules:minecraft-core"),
    ":modules:utility" to setOf(":modules:foundation", ":modules:minecraft-core"),
    ":modules:player" to setOf(":modules:foundation", ":modules:minecraft-core"),
    ":modules:application" to setOf(
        ":modules:foundation",
        ":modules:minecraft-core",
        ":modules:movement",
        ":modules:communication",
        ":modules:administration",
        ":modules:utility",
        ":modules:player",
    ),
)

tasks.register("checkModuleDependencies") {
    group = "verification"
    description = "Verifies the compile-time module dependency allowlist."
    doLast {
        val violations = mutableListOf<String>()
        dependencyAllowlist.forEach { (path, allowed) ->
            val target = rootProject.project(path)
            target.configurations
                    .getByName("implementation")
                    .dependencies
                    .withType(ProjectDependency::class.java)
                    .forEach { dependency ->
                        if (dependency.path !in allowed) {
                            violations += "$path depends on ${dependency.path}, which is not allowed"
                        }
                    }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ module dependency violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}

tasks.register("checkModules") {
    group = "verification"
    description = "Runs the checks of every compile-time architecture module."
    dependsOn(dependencyAllowlist.keys.map { "$it:check" })
}

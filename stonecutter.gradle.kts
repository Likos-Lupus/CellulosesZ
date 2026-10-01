plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2-fabric" /* [SC] DO NOT EDIT */

val architectureRoot: File = rootProject.projectDir

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Enforces CellulosesZ architecture rules on shared sources."

    val commonKotlin = architectureRoot.resolve("src/common/kotlin")
    val commonTestKotlin = architectureRoot.resolve("src/common/test/kotlin")
    val commonJava = architectureRoot.resolve("src/common/java")
    val neoforgeJava = architectureRoot.resolve("src/neoforge/java")
    inputs.files(commonKotlin, commonTestKotlin).withPropertyName("commonSources").optional()
    inputs.dir(neoforgeJava).withPropertyName("neoforgeJava").optional()

    doLast {
        val violations = mutableListOf<String>()

        if (commonJava.exists()) {
            violations += "src/common/java must not exist (common sources are Kotlin-only)"
        }

        val forbidden = linkedMapOf(
            "GlobalScope" to "GlobalScope",
            "CompletableFuture" to "CompletableFuture",
            "CompletionStage" to "CompletionStage",
            "java.util.concurrent.Executors" to "java.util.concurrent.Executors",
            "ServiceLoader" to "ServiceLoader",
            "ClassGraph" to "ClassGraph",
            "getDeclaredClasses" to "reflection class scanning",
            "printStackTrace" to "printStackTrace",
        )

        fun inspect(root: File) {
            if (!root.isDirectory) return
            root.walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                    .forEach { file ->
                        val text = file.readText()
                        val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
                        forbidden.forEach { (needle, label) ->
                            if (text.contains(needle)) {
                                violations += "$relative: forbidden $label"
                            }
                        }
                        if (text.contains("println(")) {
                            violations += "$relative: forbidden println (use SLF4J)"
                        }
                        if (text.contains("runBlocking") && !relative.contains("/test/")) {
                            violations += "$relative: forbidden runBlocking in production code"
                        }
                    }
        }

        inspect(commonKotlin)
        inspect(commonTestKotlin)

        if (neoforgeJava.isDirectory) {
            val allowlist = setOf("top/likoslupus/cellulosesz/neoforge/CellulosesZNeoForge.java")
            neoforgeJava.walkTopDown()
                    .filter { it.isFile && it.extension == "java" }
                    .forEach { file ->
                        val relative = file.relativeTo(neoforgeJava).invariantSeparatorsPath
                        if (relative !in allowlist) {
                            violations += "src/neoforge/java/$relative: Java file is not on the NeoForge allowlist"
                        }
                    }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ architecture violations:\n - " + violations.joinToString("\n - ")
            )
        }
    }
}

tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds every registered loader cell."
    dependsOn("verifyArchitecture")
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
    dependsOn("verifyArchitecture")
    dependsOn(":${stonecutter.current?.project ?: error("No active Stonecutter version")}:build")
}

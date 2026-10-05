import java.util.zip.ZipFile

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2-fabric" /* [SC] DO NOT EDIT */

val architectureRoot: File = rootProject.projectDir
val modulesDir: File = architectureRoot.resolve("modules")
val platformDir: File = architectureRoot.resolve("platform")

// Project-dependency allowlist. Any other feature -> feature edge fails the build.
val dependencyAllowlist: Map<String, Set<String>> = mapOf(
    ":modules:foundation" to emptySet(),
    ":modules:minecraft-core" to setOf(":modules:foundation"),
    ":modules:movement" to setOf(
        ":modules:foundation",
        ":modules:minecraft-core"
    ),
    ":modules:communication" to setOf(
        ":modules:foundation",
        ":modules:minecraft-core"
    ),
    ":modules:administration" to setOf(
        ":modules:foundation",
        ":modules:minecraft-core"
    ),
    ":modules:utility" to setOf(
        ":modules:foundation",
        ":modules:minecraft-core"
    ),
    ":modules:application" to setOf(
        ":modules:foundation",
        ":modules:minecraft-core",
        ":modules:movement",
        ":modules:communication",
        ":modules:administration",
        ":modules:utility",
    ),
)

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Enforces CellulosesZ source-level architecture rules across modules and loaders."

    // No declared inputs: this is a source scan and must not be tied to build output locations.

    doLast {
        val violations = mutableListOf<String>()

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
        val foundationForbidden = listOf(
            "net.minecraft.",
            "dev.architectury.",
            "net.fabricmc.",
            "net.neoforged."
        )
        val loaderForbidden = listOf("net.fabricmc.", "net.neoforged.")

        fun kotlinFiles(root: File): List<File> =
            if (!root.isDirectory) {
                emptyList()
            } else {
                root.walkTopDown()
                        .filter { it.isFile && it.extension == "kt" }
                        .toList()
            }

        fun javaFiles(root: File): List<File> =
            when {
                !root.isDirectory -> emptyList()
                else -> root.walkTopDown()
                        .filter { it.isFile && it.extension == "java" }
                        .toList()
            }

        // Java is allowed only in a `mixin` package (Mixin's Kotlin support is incomplete) and at
        // the loader shim allowlist below.
        fun isMixinPackage(relative: String): Boolean =
            relative.split('/').contains("mixin")

        fun inspect(file: File, extraForbidden: List<String> = emptyList()) {
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
            extraForbidden.forEach { needle ->
                if (text.contains(needle)) {
                    violations += "$relative: forbidden loader/Minecraft import '$needle'"
                }
            }
        }

        modulesDir.listFiles().orEmpty().filter { it.isDirectory }.forEach { module ->
            javaFiles(module.resolve("src/main/java")).forEach { file ->
                val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
                if (!isMixinPackage(relative)) {
                    violations += "$relative: Java is only allowed in a 'mixin' package"
                } else {
                    inspect(file, loaderForbidden)
                }
            }
            kotlinFiles(module.resolve("src/main/kotlin"))
                    .forEach { file ->
                        val extra = buildList {
                            addAll(loaderForbidden)
                            if (module.name == "foundation") {
                                addAll(foundationForbidden)
                            }
                        }
                        inspect(file, extra)
                    }
            kotlinFiles(module.resolve("src/test/kotlin"))
                    .forEach { file ->
                        inspect(file, emptyList())
                    }
        }

        // Single commit path: Minecraft teleport calls are allowed only in the teleport backend.
        val teleportBackendFile =
            "modules/movement/src/main/kotlin/top/likoslupus/cellulosesz/movement/teleport/MinecraftTeleportBackend.kt"
        val directTeleportMarkers = listOf(".teleport(", ".teleportTo(", "TeleportTransition(")
        modulesDir.listFiles().orEmpty().filter { it.isDirectory }.forEach { module ->
            kotlinFiles(module.resolve("src/main/kotlin")).forEach { file ->
                val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
                if (relative != teleportBackendFile) {
                    val text = file.readText()
                    directTeleportMarkers.forEach { marker ->
                        if (text.contains(marker)) {
                            violations +=
                                "$relative: forbidden direct teleport call '$marker' (route through TeleportCoordinator)"
                        }
                    }
                }
            }
        }

        // Moderation: the native ban lists may only be touched by the ban backend, and operator
        // execution/game mode/kill only by the control backend.
        val banBackendFile =
            "modules/administration/src/main/kotlin/top/likoslupus/cellulosesz/administration/moderation/ban/MinecraftBanBackend.kt"
        val controlBackendFile =
            "modules/administration/src/main/kotlin/top/likoslupus/cellulosesz/administration/operator/MinecraftPlayerControlBackend.kt"
        val banMarkers = listOf(
            "UserBanListEntry",
            "IpBanListEntry",
            "playerList.bans",
            "playerList.ipBans"
        )
        val controlMarkers = listOf(
            "player.setGameMode(",
            "player.kill(",
            "performPrefixedCommand"
        )
        val commandIoMarkers = listOf("java.nio.file.Files", "AtomicFile", "StorageJson")

        modulesDir.listFiles().orEmpty().filter { it.isDirectory }.forEach { module ->
            kotlinFiles(module.resolve("src/main/kotlin")).forEach { file ->
                val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
                val text = file.readText()

                if (relative != banBackendFile) {
                    banMarkers.forEach { marker ->
                        if (text.contains(marker)) {
                            violations +=
                                "$relative: forbidden native ban API '$marker' (use BanBackend)"
                        }
                    }
                }
                if (relative != controlBackendFile) {
                    controlMarkers.forEach { marker ->
                        if (text.contains(marker)) {
                            violations +=
                                "$relative: forbidden operator control API '$marker' (use PlayerControlBackend)"
                        }
                    }
                }

                val isCommandFile =
                    relative.contains("/command/") || file.name.endsWith("Commands.kt")
                if (isCommandFile) {
                    commandIoMarkers.forEach { marker ->
                        if (text.contains(marker)) {
                            violations += "$relative: command files must not perform file IO ('$marker')"
                        }
                    }
                }
            }
        }

        // Communication: JSON serialization and atomic writes are owned by repositories only.
        val communicationIoAllowlist = setOf(
            "modules/communication/src/main/kotlin/top/likoslupus/cellulosesz/communication/preferences/FileMessagingPreferencesRepository.kt",
            "modules/communication/src/main/kotlin/top/likoslupus/cellulosesz/communication/mail/FileMailboxRepository.kt"
        )
        kotlinFiles(
            modulesDir.resolve("communication")
                    .resolve("src/main/kotlin")
        ).forEach { file ->
            val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
            if (relative !in communicationIoAllowlist) {
                val text = file.readText()
                listOf("StorageJson", "AtomicFile").forEach { marker ->
                    if (text.contains(marker)) {
                        violations +=
                            "$relative: communication IO must live in a repository ('$marker')"
                    }
                }
            }
        }

        // Utility: JSON serialization and atomic writes are owned by kit repositories only.
        val utilityIoAllowlist = setOf(
            "modules/utility/src/main/kotlin/top/likoslupus/cellulosesz/utility/kit/FileKitRepository.kt",
            "modules/utility/src/main/kotlin/top/likoslupus/cellulosesz/utility/kit/FileKitClaimRepository.kt"
        )
        kotlinFiles(
            modulesDir.resolve("utility")
                    .resolve("src/main/kotlin")
        ).forEach { file ->
            val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
            if (relative !in utilityIoAllowlist) {
                val text = file.readText()
                listOf("StorageJson", "AtomicFile").forEach { marker ->
                    if (text.contains(marker)) {
                        violations +=
                            "$relative: utility IO must live in a repository ('$marker')"
                    }
                }
            }
        }

        // Kits: live inventory mutation is allowed only in the kit inventory backend.
        val kitInventoryBackendFile =
            "modules/utility/src/main/kotlin/top/likoslupus/cellulosesz/utility/kit/MinecraftKitInventoryBackend.kt"
        kotlinFiles(
            modulesDir.resolve("utility")
                    .resolve("src/main/kotlin")
                    .resolve("top")
                    .resolve("likoslupus")
                    .resolve("cellulosesz")
                    .resolve("utility")
                    .resolve("kit")
        ).forEach { file ->
            val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
            if (relative != kitInventoryBackendFile) {
                val text = file.readText()
                listOf(".setItem(", ".drop(").forEach { marker ->
                    if (text.contains(marker)) {
                        violations +=
                            "$relative: kit inventory mutation belongs in MinecraftKitInventoryBackend ('$marker')"
                    }
                }
            }
        }

        // Java is exception-only: classes in a `mixin` package plus the loader entrypoint shims.
        val javaAllowlist = setOf(
            "platform/fabric/src/main/java/top/likoslupus/cellulosesz/fabric/CellulosesZFabric.java",
            "platform/neoforge/src/main/java/top/likoslupus/cellulosesz/neoforge/CellulosesZNeoForge.java"
        )
        platformDir.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .forEach { file ->
                    val relative = file.relativeTo(architectureRoot).invariantSeparatorsPath
                    if (relative !in javaAllowlist && !isMixinPackage(relative)) {
                        violations += "$relative: Java file is not on the allowlist"
                    }
                }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ architecture violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}

tasks.register("checkModuleDependencies") {
    group = "verification"
    description = "Verifies the compile-time module dependency allowlist."

    doLast {
        val violations = mutableListOf<String>()
        dependencyAllowlist.forEach { (path, allowed) ->
            val target = rootProject.project(path)
            val dependencies = target.configurations
                    .getByName("implementation")
                    .dependencies
                    .withType(ProjectDependency::class.java)
            dependencies.forEach { dependency ->
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

tasks.register("inspectArtifacts") {
    group = "verification"
    description = "Inspects both loader distributions (flattened modules, no bundled externals, JIJ)."
    dependsOn("verifyArchitecture", "checkModuleDependencies")
    dependsOn(":26.1.2-fabric:build", ":26.1.2-neoforge:build")

    doLast {
        fun distributionJar(directory: File, marker: String): File =
            directory.listFiles()
                    ?.firstOrNull {
                        it.name.endsWith("$marker.jar")
                                && !it.name.contains("sources")
                    }
                ?: throw GradleException("missing $marker distribution jar in $directory")

        fun entriesOf(jar: File): Set<String> =
            ZipFile(jar).use { zip ->
                zip.entries().asSequence()
                        .map { it.name }
                        .toSet()
            }

        fun entryText(jar: File, entry: String): String? =
            ZipFile(jar).use { zip ->
                zip.getEntry(entry)?.let { zip.getInputStream(it).readBytes().decodeToString() }
            }

        val violations = mutableListOf<String>()

        val fabricJar = distributionJar(
            architectureRoot.resolve("versions/26.1.2-fabric/build/libs"),
            "fabric"
        )
        val neoJar = distributionJar(
            architectureRoot.resolve("versions/26.1.2-neoforge/build/libs"),
            "neoforge"
        )
        val fabricEntries = entriesOf(fabricJar)
        val neoEntries = entriesOf(neoJar)

        val requiredFlattened = listOf(
            "top/likoslupus/cellulosesz/application/bootstrap/CellulosesZ.class",
            "top/likoslupus/cellulosesz/movement/home/HomeService.class",
            "top/likoslupus/cellulosesz/utility/kit/FileKitRepository.class",
        )
        requiredFlattened.forEach { entry ->
            if (entry !in fabricEntries) {
                violations += "fabric jar is missing flattened class $entry"
            }
            if (entry !in neoEntries) {
                violations += "neoforge jar is missing flattened class $entry"
            }
        }

        if ("fabric.mod.json" !in fabricEntries) {
            violations += "fabric jar is missing fabric.mod.json"
        }
        val fabricModJson = entryText(fabricJar, "fabric.mod.json").orEmpty()
        if (!fabricModJson.contains("\"environment\": \"*\"")) {
            violations += "fabric.mod.json must declare \"environment\": \"*\" (client-loadable, server-gated)"
        }
        if ("META-INF/neoforge.mods.toml" !in neoEntries) {
            violations += "neoforge jar is missing neoforge.mods.toml"
        }
        if (neoEntries.none { it.startsWith("META-INF/jars/kotlin-stdlib") }) {
            violations += "neoforge jar is missing the Kotlin stdlib Jar-in-Jar"
        }

        // Externals must never be bundled; loaders must not leak into each other.
        (fabricEntries + neoEntries)
                .filter {
                    it.startsWith("net/minecraft/") || it.startsWith("net/fabricmc/") || it.startsWith(
                        "net/neoforged/"
                    )
                }
                .forEach {
                    violations += "distribution jar bundles external class $it"
                }
        if (fabricEntries.any { it.startsWith("META-INF/jars/") }) {
            violations += "fabric jar must not contain Jar-in-Jar payloads"
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ artifact inspection violations:\n - " + violations.joinToString("\n - ")
            )
        }
    }
}

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

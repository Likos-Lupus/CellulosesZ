package cellulosesz.checks

import java.io.File

/** Source-level architecture rules. Returns every violation across modules and loaders. */
fun architectureViolations(root: File, platformDir: File): List<String> {
    val violations = mutableListOf<String>()
    val modulesDir = root.resolve("modules")

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
        "net.neoforged.",
        "net.kyori.",
    )
    val loaderForbidden = listOf(
        "net.fabricmc.",
        "net.neoforged.",
        "me.lucko.",
        "net.luckperms.",
    )

    fun inspect(file: File, extraForbidden: List<String>) {
        val text = file.readText()
        val relative = relativePath(root, file)

        forbidden.forEach { (needle, label) ->
            if (text.contains(needle)) {
                violations += "$relative: forbidden $label"
            }
        }
        if (text.contains("println(")) {
            violations += "$relative: forbidden println (use SLF4J)"
        }
        if (text.contains("runBlocking")
            && !relative.contains("/test/")
        ) {
            violations += "$relative: forbidden runBlocking in production code"
        }
        extraForbidden.forEach { needle ->
            if (text.contains(needle)) {
                violations += "$relative: forbidden loader/Minecraft import '$needle'"
            }
        }
    }

    val modules = modulesDir.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
    val mainKotlin = modules
            .flatMap {
                kotlinFiles(it.resolve("src/main/kotlin"))
            }

    // Java is exception-only: a `mixin` package plus the loader-shim allowlist.
    val javaAllowlist = setOf(
        "platform/fabric/src/main/java/top/likoslupus/cellulosesz/fabric/CellulosesZFabric.java",
        "platform/neoforge/src/main/java/top/likoslupus/cellulosesz/neoforge/CellulosesZNeoForge.java",
    )

    // Forbidden APIs, foundation/loader import bans.
    modules.forEach { module ->
        javaFiles(module.resolve("src/main/java")).forEach { file ->
            val relative = relativePath(root, file)
            if (!isMixinPackage(relative)) {
                violations += "$relative: Java is only allowed in a 'mixin' package"
            } else {
                inspect(file, loaderForbidden)
            }
        }
        kotlinFiles(module.resolve("src/main/kotlin")).forEach { file ->
            val extra = buildList {
                addAll(loaderForbidden)
                if (module.name == "foundation") {
                    addAll(foundationForbidden)
                }
            }
            inspect(file, extra)
        }
        kotlinFiles(module.resolve("src/test/kotlin")).forEach { file ->
            inspect(file, emptyList())
        }
    }

    platformDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { file ->
                val relative = relativePath(root, file)
                if (relative !in javaAllowlist && !isMixinPackage(relative)) {
                    violations += "$relative: Java file is not on the allowlist"
                }
            }

    // Single teleport path.
    val teleportBackendFile =
        "modules/movement/src/main/kotlin/top/likoslupus/cellulosesz/movement/teleport/MinecraftTeleportBackend.kt"
    val directTeleportMarkers = listOf(".teleport(", ".teleportTo(", "TeleportTransition(")
    mainKotlin.forEach { file ->
        val relative = relativePath(root, file)
        if (relative != teleportBackendFile) {
            val text = file.readText()
            directTeleportMarkers.forEach { marker ->
                if (text.contains(marker)) {
                    violations += "$relative: forbidden direct teleport call '$marker' (route through TeleportCoordinator)"
                }
            }
        }
    }

    // Native ban and operator-control single backends.
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
    val controlMarkers = listOf("player.setGameMode(", "player.kill(", "performPrefixedCommand")
    mainKotlin.forEach { file ->
        val relative = relativePath(root, file)
        val text = file.readText()
        if (relative != banBackendFile) {
            banMarkers.forEach { marker ->
                if (text.contains(marker)) {
                    violations += "$relative: forbidden native ban API '$marker' (use BanBackend)"
                }
            }
        }
        if (relative != controlBackendFile) {
            controlMarkers.forEach { marker ->
                if (text.contains(marker)) {
                    violations += "$relative: forbidden operator control API '$marker' (use PlayerControlBackend)"
                }
            }
        }
    }

    // JDBC ownership: repositories/infrastructure only; Hikari in foundation only.
    mainKotlin.forEach { file ->
        val relative = relativePath(root, file)
        val text = file.readText()
        if (text.contains("java.sql.")
            && !file.name.contains("Jdbc")
            && !file.name.endsWith("Repository.kt")
            && !relative.contains("/database/")
        ) {
            violations += "$relative: JDBC access must live in a repository ('java.sql')"
        }
        if (text.contains("HikariDataSource")
            && !relative.contains("/foundation/")
        ) {
            violations += "$relative: HikariDataSource is owned by foundation only"
        }
    }

    // Adventure platform locality: only minecraft-core's text/adventure owns it.
    mainKotlin.forEach { file ->
        val relative = relativePath(root, file)
        if (file.readText().contains("net.kyori.adventure.platform.") &&
            !relative.contains("/core/text/adventure/")
        ) {
            violations += "$relative: Adventure platform API is owned by core/text/adventure"
        }
    }

    // The decorative prefix and its retired APIs must not exist in production.
    mainKotlin.forEach { file ->
        val relative = relativePath(root, file)
        val text = file.readText()
        listOf(
            "Messages.prefixed",
            "Messages.PREFIX",
            "renderPrefixedFor",
            "[CellulosesZ]"
        ).forEach { marker ->
            if (text.contains(marker)) {
                violations += "$relative: retired prefix API/marker '$marker' must not be used"
            }
        }
    }

    return violations
}

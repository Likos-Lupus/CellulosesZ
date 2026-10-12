package cellulosesz.checks

import java.io.File

/** Walks [root] for Kotlin sources; empty when the directory does not exist. */
fun kotlinFiles(root: File): List<File> =
    if (!root.isDirectory) {
        emptyList()
    } else {
        root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
    }

/** Walks [root] for Java sources; empty when the directory does not exist. */
fun javaFiles(root: File): List<File> =
    if (!root.isDirectory) {
        emptyList()
    } else {
        root.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .toList()
    }

/** Java is exception-only: allowed in a `mixin` package and at the loader-shim allowlist. */
fun isMixinPackage(relative: String): Boolean =
    relative.split('/').contains("mixin")

/** Project-relative, slash-separated path used in violation messages. */
fun relativePath(root: File, file: File): String =
    file.relativeTo(root).invariantSeparatorsPath

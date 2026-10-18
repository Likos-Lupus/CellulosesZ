package top.likoslupus.cellulosesz.foundation.config

import kotlinx.serialization.KSerializer

/**
 * One top-level scope inside a multi-file configuration. [scope] is the TOML table name, [serializer]
 * decodes exactly that table, and [defaults] is used when the table (or its whole file) is absent.
 */
public class ConfigScopeSpec<T : Any>(
    public val scope: String,
    public val serializer: KSerializer<T>,
    public val defaults: T,
)

public fun <T : Any> configScope(
    scope: String,
    serializer: KSerializer<T>,
    defaults: T,
): ConfigScopeSpec<T> {
    require(scope.isNotBlank()) { "config scope must not be blank" }
    return ConfigScopeSpec(scope, serializer, defaults)
}

/**
 * A configuration file that owns one or more top-level scopes. A file is an independent unit: only
 * the scopes listed here are decoded from it, so a table owned by another file is ignored rather
 * than merged.
 */
public class ConfigFileSpec internal constructor(
    public val fileName: String,
    public val templateResource: String,
    public val scopes: List<ConfigScopeSpec<*>>,
) {

    public val allowedScopes: Set<String> = scopes.map { it.scope }.toSet()

}

public fun configFile(
    fileName: String,
    templateResource: String,
    vararg scopes: ConfigScopeSpec<*>,
): ConfigFileSpec {
    require(fileName.isNotBlank()) {
        "config file name must not be blank"
    }
    require(templateResource.startsWith("/")) {
        "template resource '$templateResource' must be an absolute classpath resource"
    }
    require(scopes.isNotEmpty()) {
        "config file '$fileName' must own at least one scope"
    }
    val names = scopes.map { it.scope }
    require(names.toSet().size == names.size) {
        "config file '$fileName' declares a duplicate scope"
    }
    return ConfigFileSpec(
        fileName,
        templateResource,
        scopes.toList()
    )
}

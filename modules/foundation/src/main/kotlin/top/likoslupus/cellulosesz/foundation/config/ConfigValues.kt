package top.likoslupus.cellulosesz.foundation.config

/** The typed scopes decoded from one configuration load, addressed by [ConfigScopeSpec]. */
public class ConfigValues internal constructor(
    private val values: Map<String, Any>,
) {

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(spec: ConfigScopeSpec<T>): T =
        (values[spec.scope] as? T)
            ?: error("config scope '${spec.scope}' was not loaded")

}

package top.likoslupus.cellulosesz.application.config

import kotlinx.serialization.Serializable

/**
 * Static command-registration settings. Every field is read once, when the dispatcher is built, so
 * changing any of them needs a restart; a reload that would alter them is rejected by
 * [ConfigValidation].
 *
 * [shortNamesEnabled] and [aliasesEnabled] only gate the optional short/alias routes. The namespaced
 * form `/cellulosesz:<command>` is always registered and unaffected.
 *
 * [vanillaHelpOverlayEnabled] is the compatibility-first controlled exception: when true,
 * CellulosesZ enhances vanilla `/help <query>` for its own commands only, and never touches the
 * no-argument `/help`. It stays enabled by default so that existing servers keep their `/help`
 * experience; it can be turned off once operators adopt the self-rooted `/cellulosesz` entry.
 */
@Serializable
public data class CommandRoutingSettings(
    public val shortNamesEnabled: Boolean = true,
    public val aliasesEnabled: Boolean = true,
    public val vanillaHelpOverlayEnabled: Boolean = true,
    public val vanillaOverrides: VanillaOverrideSettings = VanillaOverrideSettings(),
    /** Canonical command name -> exact decimal cost, charged through the economy service. */
    public val costs: Map<String, String> = emptyMap(),
    /** Canonical command name -> cooldown in seconds; `"*"` is the default for every command. */
    public val cooldowns: Map<String, Int> = emptyMap(),
    public val cooldownPersistence: Boolean = true,
    /** Canonical CellulosesZ command names that must not be registered. Startup binding. */
    public val disabled: List<String> = emptyList(),
)

/**
 * Taking over vanilla command literals (EssentialsX-style): on a name collision CellulosesZ owns
 * the plain literal and the vanilla command stays reachable at `/minecraft:<literal>`. Enabled by
 * default; disabling it leaves every vanilla literal alone and keeps only the `cz`/namespaced
 * fallbacks. [commands] restricts the takeover to the listed literals — an empty list means every
 * catalogued command, and only [TakeoverSupport.VERIFIED] targets are eligible (see
 * [VanillaCommandCatalog]). A change needs a restart and cannot reload.
 */
@Serializable
public data class VanillaOverrideSettings(
    public val enabled: Boolean = true,
    public val commands: List<String> = emptyList(),
)

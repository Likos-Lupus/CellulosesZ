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
)

/**
 * Explicit opt-in for taking over vanilla command literals. Disabled by default so a fresh install
 * always leaves `/me`, `/tp`, ... to vanilla. [commands] is an allowlist of vanilla-conflicting
 * literals CellulosesZ may take over; only [TakeoverSupport.VERIFIED] targets are accepted (see
 * [VanillaCommandCatalog]). A change needs a restart and cannot reload.
 */
@Serializable
public data class VanillaOverrideSettings(
    public val enabled: Boolean = false,
    public val commands: List<String> = emptyList(),
)

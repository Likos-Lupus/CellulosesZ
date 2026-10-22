package top.likoslupus.cellulosesz.core.command.registry

import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * How far CellulosesZ's takeover of a vanilla-conflicting command has been verified for the target
 * version. Only [VERIFIED] targets may be enabled for takeover through configuration; [CANDIDATE]
 * targets are catalogued and receive a protected `cz` fallback but must not be configured until the
 * target-version spike passes; [NOT_SUPPORTED] targets are fallback-only.
 */
public enum class TakeoverSupport {

    VERIFIED,
    CANDIDATE,
    NOT_SUPPORTED,

}

/**
 * The audited set of vanilla commands whose literal collides with a CellulosesZ command. Each one
 * keeps a protected single-segment fallback (`cz` + the vanilla literal) that is declared here and
 * is independent of the short-name and alias switches.
 *
 * The fallback prefix is never a `/cz` root command; `cztp`, `czme`, ... are single segments.
 */
public enum class VanillaCommandId(
    public val literal: String,
    public val takeoverSupport: TakeoverSupport,
) {

    ME("me", TakeoverSupport.VERIFIED),
    TP("tp", TakeoverSupport.CANDIDATE),
    KILL("kill", TakeoverSupport.NOT_SUPPORTED),
    GAMEMODE("gamemode", TakeoverSupport.NOT_SUPPORTED);

    /** The protected fallback literal: always `cz` followed by the vanilla literal. */
    public val fallbackLiteral: String
        get() = "cz$literal"

}

/** Pure, audited lookup from a CellulosesZ canonical name to its vanilla conflict, if any. */
public object VanillaCommandCatalog {

    private val byLiteral: Map<String, VanillaCommandId> =
        VanillaCommandId.entries.associateBy(VanillaCommandId::literal)

    public fun forCanonical(name: String): VanillaCommandId? =
        byLiteral[name]

    public val all: List<VanillaCommandId>
        get() = VanillaCommandId.entries

    /**
     * A problem description when [literal] cannot be configured for takeover, or null when it can.
     * Unknown names and unverified/non-supported targets are rejected with a precise message.
     */
    public fun takeoverProblem(literal: String): String? {
        val command = byLiteral[literal]
            ?: return "unknown vanilla command '$literal'"
        return when (command.takeoverSupport) {
            TakeoverSupport.VERIFIED ->
                null

            TakeoverSupport.CANDIDATE ->
                "the vanilla /$literal takeover is not verified for this version yet; use /${command.fallbackLiteral}"

            TakeoverSupport.NOT_SUPPORTED ->
                "the vanilla /$literal takeover is not supported; use /${command.fallbackLiteral}"
        }
    }

    /**
     * The extra permission a route-level takeover requires, or null when the vanilla command itself
     * is an ordinary player command (only `/me` today). This guards the vanilla management entry:
     * taking over `/tp` must not widen the ordinary player's access.
     */
    public fun overridePermission(command: VanillaCommandId): PermissionSpec? =
        when (command) {
            VanillaCommandId.TP -> CommandPermissions.TP_VANILLA_OVERRIDE
            else -> null
        }

}

/** Who owns a vanilla-conflicting literal for the current startup. */
public enum class VanillaRouteMode {

    /** Vanilla (or whichever mod owns it) keeps the literal; CellulosesZ stays on `cz`/namespaced. */
    PRESERVE_VANILLA,

    /** CellulosesZ suppresses the vanilla registration and installs its own literal. */
    TAKE_OVER_VANILLA,

    /** The owning feature is disabled; CellulosesZ registers neither the literal nor a fallback. */
    FEATURE_DISABLED,

}

/** The resolved ownership of one catalogued command for the current startup. */
public data class EffectiveVanillaRoute(
    public val command: VanillaCommandId,
    public val mode: VanillaRouteMode,
    public val fallbackLiteral: String,
)

/**
 * Pure decision for one catalogued command. A takeover requires the feature to be enabled and the
 * global override switch to be on; an empty [overrides] allowlist means every catalogued command,
 * while a non-empty list restricts the takeover to its members. Takeover support is validated
 * separately (the policy only promotes [TakeoverSupport.VERIFIED] targets).
 */
public fun effectiveVanillaRoute(
    command: VanillaCommandId,
    featureEnabled: Boolean,
    overridesEnabled: Boolean,
    overrides: Set<String>,
): EffectiveVanillaRoute =
    EffectiveVanillaRoute(
        command = command,
        mode = when {
            !featureEnabled ->
                VanillaRouteMode.FEATURE_DISABLED

            !overridesEnabled ->
                VanillaRouteMode.PRESERVE_VANILLA

            overrides.isNotEmpty() && command.literal !in overrides ->
                VanillaRouteMode.PRESERVE_VANILLA

            else ->
                VanillaRouteMode.TAKE_OVER_VANILLA
        },
        fallbackLiteral = command.fallbackLiteral,
    )

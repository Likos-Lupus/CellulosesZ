package top.likoslupus.cellulosesz.api.extension

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.actor.AddonDescriptor
import top.likoslupus.cellulosesz.api.presentation.HelpContributionResult
import top.likoslupus.cellulosesz.api.presentation.HelpEntryDescriptor

/** Invoked when the startup registration window opens; independent of server-session READY. */
public fun interface ExtensionRegistrationListener {

    public fun onRegistrationOpen(registration: AddonRegistration)

}

/**
 * The startup contribution window. It exposes only the few well-defined contribution types — it is
 * not a service registry and offers no `getService(Class)`.
 *
 * Policy contributions (teleport kit, eligibility) should be registered before the first server
 * session; late policy registration is rejected so a protection rule can never activate after the
 * first player action it should have guarded.
 */
public interface AddonRegistration {

    public fun registerAddon(descriptor: AddonDescriptor): AddonAccess

    public fun registerTeleportPolicy(
        access: AddonAccess,
        checker: TeleportPolicyChecker,
    ): Subscription

    public fun registerKitEligibility(
        access: AddonAccess,
        checker: KitEligibilityChecker,
    ): Subscription

    public fun registerHelpDocument(
        access: AddonAccess,
        descriptor: HelpEntryDescriptor,
    ): HelpContributionResult

    /**
     * Registers this addon's own translations, keyed by locale (`xx_yy`) then by the addon-local key
     * used in `AddonTextKey`. A malformed template or an unknown locale is rejected. Returns how many
     * entries were accepted.
     */
    public fun registerTranslations(
        access: AddonAccess,
        translations: Map<String, Map<String, String>>,
    ): TranslationRegistrationResult

}

/** The outcome of [AddonRegistration.registerTranslations]. */
public sealed interface TranslationRegistrationResult {

    public data class Registered(public val accepted: Int) : TranslationRegistrationResult

    public data object NotInWindow : TranslationRegistrationResult

    public data class Rejected(public val reason: String) : TranslationRegistrationResult

}

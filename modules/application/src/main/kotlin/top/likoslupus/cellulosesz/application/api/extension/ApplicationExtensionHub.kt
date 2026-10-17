package top.likoslupus.cellulosesz.application.api.extension

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.actor.AddonDescriptor
import top.likoslupus.cellulosesz.api.extension.AddonRegistration
import top.likoslupus.cellulosesz.api.extension.KitEligibilityChecker
import top.likoslupus.cellulosesz.api.extension.TeleportPolicyChecker
import top.likoslupus.cellulosesz.api.extension.TranslationRegistrationResult
import top.likoslupus.cellulosesz.api.presentation.HelpContributionResult
import top.likoslupus.cellulosesz.api.presentation.HelpEntryDescriptor
import top.likoslupus.cellulosesz.application.api.ApiAuthorization
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId

/**
 * The startup contribution window. It is the only place addons can register policy/help
 * contributions. It holds immutable, ordered collections and never exposes a generic service
 * registry, repository or Minecraft object.
 *
 * The window opens during mod init and closes at the first `SERVER_STARTING`; a policy registered
 * after that is rejected (not silently applied late), so a protection rule can never activate after
 * the first player action it should have guarded.
 */
internal class ApplicationExtensionHub(
    private val authorization: ApiAuthorization,
    private val addonText: AddonMessageOverlay,
) : AddonRegistration {

    private val lock = Any()
    private var open = true
    private var sequence = 0L
    private val addons = LinkedHashMap<String, AddonAccess>()
    private val teleportPolicies = LinkedHashMap<Long, Pair<AddonAccess, TeleportPolicyChecker>>()
    private val kitEligibilities = LinkedHashMap<Long, Pair<AddonAccess, KitEligibilityChecker>>()
    private val helpDocuments = LinkedHashMap<String, HelpEntryDescriptor>()

    fun closeWindow() {
        synchronized(lock) {
            open = false
        }
    }

    override fun registerAddon(descriptor: AddonDescriptor): AddonAccess =
        synchronized(lock) {
            addons.getOrPut(descriptor.id) {
                authorization.access(descriptor.id)
            }
        }

    override fun registerTeleportPolicy(
        access: AddonAccess,
        checker: TeleportPolicyChecker,
    ): Subscription =
        registerPolicy(
            access,
            checker,
            teleportPolicies
        )

    override fun registerKitEligibility(
        access: AddonAccess,
        checker: KitEligibilityChecker,
    ): Subscription =
        registerPolicy(
            access,
            checker,
            kitEligibilities
        )

    override fun registerHelpDocument(
        access: AddonAccess,
        descriptor: HelpEntryDescriptor,
    ): HelpContributionResult =
        synchronized(lock) {
            when {
                !open ->
                    HelpContributionResult.RegistrationClosed

                descriptor.documentId.isBlank() || descriptor.commandName.isBlank() ->
                    HelpContributionResult.InvalidKey

                helpDocuments.containsKey(descriptor.documentId) ->
                    HelpContributionResult.Conflict

                else -> {
                    helpDocuments[descriptor.documentId] = descriptor
                    HelpContributionResult.Registered
                }
            }
        }


    override fun registerTranslations(
        access: AddonAccess,
        translations: Map<String, Map<String, String>>,
    ): TranslationRegistrationResult =
        synchronized(lock) {
            if (!open) {
                return@synchronized TranslationRegistrationResult.NotInWindow
            }
            var accepted = 0
            var rejected = 0
            translations.forEach { (locale, entries) ->
                val language = LanguageId.parse(locale)
                entries.forEach { (key, source) ->
                    if (language != null &&
                        addonText.register(access.addonId, key, language, source)
                    ) {
                        accepted++
                    } else {
                        rejected++
                    }
                }
            }
            when {
                accepted > 0 -> TranslationRegistrationResult.Registered(accepted)
                rejected > 0 -> TranslationRegistrationResult.Rejected("no valid translations")
                else -> TranslationRegistrationResult.Registered(0)
            }
        }

    fun registeredAddonIds(): List<String> =
        synchronized(lock) { addons.keys.toList() }

    /**
     * Server-side authorization for a privileged operation: the caller must present the exact
     * [AddonAccess] instance this hub registered for its id (a self-implemented handle is rejected),
     * and the live configuration must grant [capability].
     */
    fun isAuthorizedAccess(access: AddonAccess, capability: String): Boolean =
        synchronized(lock) {
            addons[access.addonId] === access &&
                    authorization.capabilitiesFor(access.addonId).contains(capability)
        }

    fun teleportPolicies(): List<TeleportPolicyChecker> =
        synchronized(lock) { teleportPolicies.values.map { it.second } }

    fun kitEligibilities(): List<KitEligibilityChecker> =
        synchronized(lock) { kitEligibilities.values.map { it.second } }

    fun helpDocuments(): List<HelpEntryDescriptor> =
        synchronized(lock) { helpDocuments.values.toList() }

    private fun <T> registerPolicy(
        access: AddonAccess,
        checker: T,
        into: LinkedHashMap<Long, Pair<AddonAccess, T>>,
    ): Subscription {
        var assigned: Long? = null
        synchronized(lock) {
            if (open) {
                val key = ++sequence
                into[key] = access to checker
                assigned = key
            }
        }
        val key = assigned
            ?: return warnRejected(access)
        return Subscription {
            synchronized(lock) { into.remove(key) }
        }
    }

    private fun warnRejected(access: AddonAccess): Subscription {
        LOGGER.warn(
            "extension_registration_rejected addon={} reason=window_closed",
            access.addonId,
        )
        return Subscription { }
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ApplicationExtensionHub::class.java)

    }

}

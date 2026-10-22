package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.platform.Platform
import net.minecraft.world.level.storage.LevelResource
import top.likoslupus.cellulosesz.administration.AdministrationFeature
import top.likoslupus.cellulosesz.application.api.extension.AddonMessageOverlay
import top.likoslupus.cellulosesz.application.config.ConfigDefaults
import top.likoslupus.cellulosesz.application.config.ConfigFiles
import top.likoslupus.cellulosesz.application.config.ConfigValidation
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.communication.CommunicationFeature
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.JdbcPlayerIdentityRepository
import top.likoslupus.cellulosesz.core.preference.JdbcPlayerPreferenceRepository
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.*
import top.likoslupus.cellulosesz.core.text.i18n.preference.JdbcPlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.presentation.createTextPresentation
import top.likoslupus.cellulosesz.core.text.theme.TextThemeResolver
import top.likoslupus.cellulosesz.economy.EconomyFeature
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.describe
import top.likoslupus.cellulosesz.foundation.database.DeferredDatabaseRuntime
import top.likoslupus.cellulosesz.movement.MovementFeature
import top.likoslupus.cellulosesz.player.PlayerFeature
import top.likoslupus.cellulosesz.utility.UtilityFeature
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers

/**
 * Bootstrap phase one: the typed config store, the runtime kernel, the storage handle and the shared
 * text/preference services every feature depends on. Config is loaded (and validated) before any
 * other service is constructed.
 */
internal object BootstrapConfiguration {

    fun create(platform: PlatformServices): BootstrapState {
        val health = ApplicationHealth().apply {
            loader = platform.loaderName
            permissionBackend = platform.permissionBridge.backendName
        }
        val kernel = RuntimeKernel()
        val permissions = PermissionService(platform.permissionBridge)

        val configDirectory = configDirectory()
        ConfigDefaults.writeIfMissing(configDirectory, ConfigFiles.all)
        val config = ConfigFiles.store(
            directory = configDirectory,
            ioDispatcher = Dispatchers.IO,
            validateTransition = ConfigValidation.rootTransition { dataRoot(kernel) },
        )
        when (val initial = config.initialLoadBlocking()) {
            is ConfigReloadResult.Success -> Unit
            is ConfigReloadResult.Failure -> {
                val message = initial.errors.joinToString("; ") { it.describe() }
                health.fail("initial config load failed: $message")
                error("initial config load failed: $message")
            }
        }
        health.transition(ApplicationHealth.State.CONFIG_READY)

        val database = DeferredDatabaseRuntime()
        val namespace = config.current.database.namespace
        val known = MinecraftKnownPlayerResolver(kernel)
        val identityRepository = JdbcPlayerIdentityRepository(database, namespace)

        val catalog = TranslationCatalog.load(
            listOf(
                TranslationSource("core", BundledTranslations::class.java),
                TranslationSource("movement", MovementFeature::class.java),
                TranslationSource("communication", CommunicationFeature::class.java),
                TranslationSource("administration", AdministrationFeature::class.java),
                TranslationSource("utility", UtilityFeature::class.java),
                TranslationSource("player", PlayerFeature::class.java),
                TranslationSource("economy", EconomyFeature::class.java),
            )
        )
        val languageRepository = JdbcPlayerLanguagePreferenceRepository(database, namespace)
        val languagePreferences = PlayerLanguagePreferences(languageRepository)
        // One shared preference service for every feature toggle. The default map is a function so a
        // reload can move the default for a player who never expressed a preference.
        val preferenceRepository = JdbcPlayerPreferenceRepository(database, namespace)
        val preferences = PlayerPreferenceService(preferenceRepository) {
            val current = config.current
            mapOf(
                PlayerPreference.TELEPORT_ENABLED to
                        current.movement.requests.acceptByDefault,
                PlayerPreference.ACCEPTING_PAY to
                        current.economy.pay.acceptByDefault,
                PlayerPreference.PAY_CONFIRMATION to
                        current.economy.pay.confirmByDefault,
                PlayerPreference.REPLY_TO_LAST_RECIPIENT to
                        current.messaging.privateMessages.defaultReplyRecipient,
                PlayerPreference.CLEAR_INVENTORY_CONFIRMATION to
                        current.utility.itemEditing.confirmClear,
            )
        }
        val languageResolver = LanguageResolver(catalog, languagePreferences) {
            config.current.localization
        }
        val presentationTheme = { TextThemeResolver.resolve(config.current.text) }
        val addonText = AddonMessageOverlay()
        val messages = LocalizedMessages(
            catalog,
            languageResolver,
            presentationTheme,
            addonText,
        )
        val presentation = createTextPresentation(
            messages,
            languageResolver,
            presentationTheme
        )
        val adventure = AdventureRuntime()

        return BootstrapState(
            platform = platform,
            health = health,
            kernel = kernel,
            permissions = permissions,
            configDirectory = configDirectory,
            config = config,
            database = database,
            namespace = namespace,
            known = known,
            identityRepository = identityRepository,
            catalog = catalog,
            languageRepository = languageRepository,
            languagePreferences = languagePreferences,
            preferenceRepository = preferenceRepository,
            preferences = preferences,
            languageResolver = languageResolver,
            presentationTheme = presentationTheme,
            addonText = addonText,
            messages = messages,
            presentation = presentation,
            adventure = adventure,
        )
    }

    fun configDirectory(): Path =
        Platform.getConfigFolder()
                .resolve("cellulosesz")

    /** The game/server directory; the driver cache lives underneath it, not under the configs. */
    fun gameRoot(): Path =
        Platform.getGameFolder()

    fun modVersion(): String =
        runCatching { Platform.getMod("cellulosesz").version }
                .getOrDefault("unknown")

    fun dataRoot(kernel: RuntimeKernel): Path =
        kernel.requireServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("cellulosesz")

}

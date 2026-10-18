package top.likoslupus.cellulosesz.application.config

import kotlinx.coroutines.CoroutineDispatcher
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.core.text.TextSettings
import top.likoslupus.cellulosesz.core.text.i18n.LocalizationSettings
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.foundation.config.BundleConfigStore
import top.likoslupus.cellulosesz.foundation.config.ConfigFileSpec
import top.likoslupus.cellulosesz.foundation.config.ConfigValues
import top.likoslupus.cellulosesz.foundation.config.ValidationError
import top.likoslupus.cellulosesz.foundation.config.configFile
import top.likoslupus.cellulosesz.foundation.config.configScope
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.player.config.PlayerSettings
import top.likoslupus.cellulosesz.utility.config.UtilitySettings
import java.nio.file.Path

/**
 * The multi-file configuration layout: the main `cellulosesz.toml` owns the foundational/ops scopes,
 * and each feature module owns exactly one file. A scope is decoded only from its owning file, so a
 * table placed in the wrong file is ignored instead of merged.
 */
internal object ConfigFiles {

    val diagnostics = configScope(
        "diagnostics",
        DiagnosticsConfig.serializer(),
        DiagnosticsConfig(),
    )
    val database = configScope(
        "database",
        DatabaseSettings.serializer(),
        DatabaseSettings(),
    )
    val localization = configScope(
        "localization",
        LocalizationSettings.serializer(),
        LocalizationSettings(),
    )
    val text = configScope(
        "text",
        TextSettings.serializer(),
        TextSettings(),
    )
    val commands = configScope(
        "commands",
        CommandRoutingSettings.serializer(),
        CommandRoutingSettings(),
    )
    val api = configScope(
        "api",
        ApiIntegrationSettings.serializer(),
        ApiIntegrationSettings(),
    )
    val backup = configScope(
        "backup",
        BackupSettings.serializer(),
        BackupSettings(),
    )

    val movement = configScope(
        "movement",
        MovementSettings.serializer(),
        MovementSettings(),
    )
    val messaging = configScope(
        "messaging",
        MessagingSettings.serializer(),
        MessagingSettings(),
    )
    val administration = configScope(
        "administration",
        AdministrationSettings.serializer(),
        AdministrationSettings(),
    )
    val utility = configScope(
        "utility",
        UtilitySettings.serializer(),
        UtilitySettings(),
    )
    val players = configScope(
        "players",
        PlayerSettings.serializer(),
        PlayerSettings(),
    )
    val economy = configScope(
        "economy",
        EconomySettings.serializer(),
        EconomySettings(),
    )

    val mainFile = configFile(
        "cellulosesz.toml",
        "/defaults/config/cellulosesz.toml",
        diagnostics,
        database,
        localization,
        text,
        commands,
        api,
        backup,
    )
    val movementFile = configFile(
        "movement.toml",
        "/defaults/config/movement.toml",
        movement,
    )
    val messagingFile = configFile(
        "messaging.toml",
        "/defaults/config/messaging.toml",
        messaging,
    )
    val administrationFile = configFile(
        "administration.toml",
        "/defaults/config/administration.toml",
        administration,
    )
    val utilityFile = configFile(
        "utility.toml",
        "/defaults/config/utility.toml",
        utility,
    )
    val playersFile = configFile(
        "players.toml",
        "/defaults/config/players.toml",
        players,
    )
    val economyFile = configFile(
        "economy.toml",
        "/defaults/config/economy.toml",
        economy,
    )

    val all: List<ConfigFileSpec> = listOf(
        mainFile,
        movementFile,
        messagingFile,
        administrationFile,
        utilityFile,
        playersFile,
        economyFile,
    )

    fun compose(values: ConfigValues): CellulosesConfig =
        CellulosesConfig(
            diagnostics = values[diagnostics],
            database = values[database],
            localization = values[localization],
            text = values[text],
            movement = values[movement],
            messaging = values[messaging],
            administration = values[administration],
            utility = values[utility],
            players = values[players],
            economy = values[economy],
            commands = values[commands],
            api = values[api],
            backup = values[backup],
        )

    fun store(
        directory: Path,
        ioDispatcher: CoroutineDispatcher,
        validateTransition: (CellulosesConfig, CellulosesConfig) -> List<ValidationError>,
    ): BundleConfigStore<CellulosesConfig> =
        BundleConfigStore(
            directory = directory,
            files = all,
            compose = ::compose,
            validate = ConfigValidation::validate,
            validateTransition = validateTransition,
            ioDispatcher = ioDispatcher,
        )

}

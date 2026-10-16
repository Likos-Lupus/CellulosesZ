package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.config.ActionMessageSettings
import top.likoslupus.cellulosesz.communication.config.MailSettings
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.core.text.TextColorOverrides
import top.likoslupus.cellulosesz.core.text.TextSettings
import top.likoslupus.cellulosesz.core.text.i18n.LocalizationSettings
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.MySqlSettings
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.movement.config.*
import top.likoslupus.cellulosesz.utility.config.KitSettings
import top.likoslupus.cellulosesz.utility.config.UtilitySettings
import java.nio.file.Path

class ConfigValidationTest {

    @Test
    fun `accepts default config`() {
        assertTrue(ConfigValidation.validate(CellulosesConfig()).isEmpty())
    }

    @Test
    fun `rejects invalid namespace`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                database = DatabaseSettings(
                    namespace = "Bad Namespace"
                )
            )
        )

        assertTrue(errors.any { it.path == "database.namespace" })
    }

    @Test
    fun `rejects invalid remote port`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                database = DatabaseSettings(
                    mysql = MySqlSettings(port = 0)
                )
            )
        )

        assertTrue(errors.any { it.path == "database.mysql.port" })
    }

    @Test
    fun `rejects password and password-env together`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                database = DatabaseSettings(
                    mysql = MySqlSettings(
                        password = "x",
                        passwordEnv = "Y"
                    )
                )
            )
        )

        assertTrue(errors.any { it.path == "database.mysql" })
    }

    @Test
    fun `rejects negative max homes`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    homes = HomeSettings(maxPerPlayer = -1)
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.homes.maxPerPlayer" })
    }

    @Test
    fun `rejects non positive teleport timeout`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    requests = TeleportRequestSettings(timeoutSeconds = 0)
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.requests.timeoutSeconds" })
    }

    @Test
    fun `rejects out of range safety radius`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    teleport = TeleportSettings(
                        safety = TeleportSafetySettings(searchHorizontalRadius = 999)
                    )
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.teleport.safety.searchHorizontalRadius" })
    }

    @Test
    fun `rejects negative teleport delay`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    teleport = TeleportSettings(
                        delaySeconds = -1
                    )
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.teleport.delaySeconds" })
    }

    @Test
    fun `rejects non positive messaging mailbox limit`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                messaging = MessagingSettings(
                    mail = MailSettings(maxMessagesPerMailbox = 0)
                )
            )
        )

        assertTrue(errors.any { it.path == "messaging.mail.maxMessagesPerMailbox" })
    }

    @Test
    fun `rejects non positive utility kit cap`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                utility = UtilitySettings(
                    kits = KitSettings(maxKits = 0)
                )
            )
        )

        assertTrue(errors.any { it.path == "utility.kits.maxKits" })
    }

    @Test
    fun `rejects an unsupported default language`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                localization = LocalizationSettings(defaultLanguage = "fr_fr")
            )
        )

        assertTrue(errors.any { it.path == "localization.defaultLanguage" })
    }

    @Test
    fun `rejects a malformed default language`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                localization = LocalizationSettings(defaultLanguage = "english")
            )
        )

        assertTrue(errors.any { it.path == "localization.defaultLanguage" })
    }

    @Test
    fun `rejects an invalid body color`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                text = TextSettings(colors = TextColorOverrides(body = "red"))
            )
        )

        assertTrue(errors.any { it.path == "text.colors.body" })
    }

    @Test
    fun `rejects an invalid secondary color`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                text = TextSettings(
                    colors = TextColorOverrides(secondary = "#GGGGGG")
                )
            )
        )

        assertTrue(errors.any { it.path == "text.colors.secondary" })
    }

    @Test
    fun `database transition is rejected when the active endpoint changes`() {
        val root = Path.of("/tmp/cz")

        val errors = ConfigValidation.validateDatabaseTransition(
            previous = DatabaseDescriptorResolver.resolve(
                DatabaseSettings(),
                root,
            ),
            candidate = DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    sqlite = SqliteSettings(
                        path = "other.db"
                    )
                ),
                root,
            ),
        )

        assertEquals("database", errors.single().path)
    }

    @Test
    fun `database transition allows changing an inactive backend section`() {
        val root = Path.of("/tmp/cz")

        val errors = ConfigValidation.validateDatabaseTransition(
            previous = DatabaseDescriptorResolver.resolve(
                DatabaseSettings(),
                root,
            ),
            candidate = DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    mysql = MySqlSettings(host = "db.example.com")
                ),
                root,
            ),
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `changing command registration settings requires a restart`() {
        val errors = ConfigValidation.validateCommandTransition(
            previous = CellulosesConfig(),
            candidate = CellulosesConfig(
                commands = CommandRoutingSettings(shortNamesEnabled = false)
            ),
        )

        assertEquals("commands", errors.single().path)
    }

    @Test
    fun `changing the effective me ownership requires a restart`() {
        val errors = ConfigValidation.validateCommandTransition(
            previous = CellulosesConfig(),
            candidate = CellulosesConfig(
                commands = CommandRoutingSettings(
                    vanillaOverrides = VanillaOverrideSettings(
                        enabled = true,
                        commands = listOf("me"),
                    )
                )
            ),
        )

        assertEquals("commands", errors.single().path)
    }

    @Test
    fun `enabling or disabling the action service requires a restart`() {
        val errors = ConfigValidation.validateCommandTransition(
            previous = CellulosesConfig(),
            candidate = CellulosesConfig(
                messaging = MessagingSettings(
                    actionMessage = ActionMessageSettings(enabled = false)
                )
            ),
        )

        assertEquals("messaging.actionMessage", errors.single().path)
    }

    @Test
    fun `changing only the action message policy stays reloadable`() {
        val errors = ConfigValidation.validateCommandTransition(
            previous = CellulosesConfig(),
            candidate = CellulosesConfig(
                messaging = MessagingSettings(
                    actionMessage = ActionMessageSettings(maximumLength = 512)
                )
            ),
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `the combined transition validator keeps the database check`() {
        val root = Path.of("/tmp/cz")
        val validator = ConfigValidation.rootTransition { root }

        val errors = validator(
            CellulosesConfig(),
            CellulosesConfig(
                database = DatabaseSettings(sqlite = SqliteSettings(path = "other.db")),
                commands = CommandRoutingSettings(aliasesEnabled = false),
            ),
        )

        assertTrue(errors.any { it.path == "database" })
        assertTrue(errors.any { it.path == "commands" })
    }

}

package top.likoslupus.cellulosesz.foundation.database.driver

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.DatabaseType
import top.likoslupus.cellulosesz.foundation.database.DriverSettings
import java.nio.file.Path

class JdbcDriverProvisioningTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `a missing driver reports the expected file, source and checksum`() {
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(type = DatabaseType.POSTGRESQL),
            tempDir,
        )
        val provisioner = JdbcDriverProvisioner.of(
            catalog = DriverCatalog.load(),
            settings = DriverSettings(autoDownload = false),
            store = DriverStore(tempDir.resolve("drivers")),
        )

        val result = provisioner.provision(descriptor)

        assertTrue(result is DriverProvision.Unavailable)
        val message = (result as DriverProvision.Unavailable).message
        assertTrue(message.contains("expected file"), message)
        assertTrue(message.contains("postgresql-"), message)
        assertTrue(message.contains("sha256"), message)
        assertTrue(message.contains("auto-download"), message)
    }

}

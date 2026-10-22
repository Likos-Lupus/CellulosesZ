package top.likoslupus.cellulosesz.foundation.database.driver

import org.h2.Driver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

/**
 * Proves the driver really comes from the external jar: the child loader's parent is the platform
 * loader, which cannot see the test class path (where H2 is also present).
 */
class IsolatedDriverLoadingTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `a driver loaded in isolation connects and pools through Hikari`() {
        val jar = Path.of(Driver::class.java.protectionDomain.codeSource.location.toURI())
        val artifact = DriverArtifact(
            coordinate = "com.h2database:h2:test",
            fileName = jar.fileName.toString(),
            size = Files.size(jar),
            sha256 = HexFormat.of().formatHex(
                MessageDigest
                        .getInstance("SHA-256")
                        .digest(Files.readAllBytes(jar))
            ),
            repositoryPath = "com/h2database/h2/test/${jar.fileName}",
        )

        val driver = IsolatedDriverLoader.loadWith(
            artifact,
            jar,
            "org.h2.Driver",
            ClassLoader.getPlatformClassLoader(),
        )
        assertNotSame(Driver::class.java, driver.javaClass)

        val dataSource = DriverDataSource(
            driver = driver,
            jdbcUrl = "jdbc:h2:mem:isolated;DB_CLOSE_DELAY=-1",
            username = "sa",
            password = "",
        )
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT 42").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(42, rows.getInt(1))
                }
            }
        }

        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(
                type = DatabaseType.H2,
                h2 = H2Settings(path = tempDir.resolve("unused").toString()),
            ),
            tempDir,
        )
        HikariDatabaseRuntime.create(descriptor, dataSource).use { runtime ->
            val value = runtime.read { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 7").use { rows ->
                        rows.next()
                        rows.getInt(1)
                    }
                }
            }
            assertEquals(7, value)
        }
    }

}

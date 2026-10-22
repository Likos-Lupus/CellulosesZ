package top.likoslupus.cellulosesz.foundation.database.driver

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.foundation.database.DatabaseType

class DriverCatalogTest {

    @Test
    fun `the packaged manifest pins one artifact per backend`() {
        val catalog = DriverCatalog.load()

        DatabaseType.entries.forEach { type ->
            val artifact = catalog.artifact(type)
            assertNotNull(artifact, "no driver manifest entry for $type")
            artifact!!
            assertEquals(
                64,
                artifact.sha256.length,
                "malformed SHA-256 for $type"
            )
            assertTrue(
                artifact.size > 0,
                "empty size for $type"
            )
            assertTrue(
                artifact.repositoryPath.endsWith(artifact.fileName),
                "repository path does not end with the file name for $type",
            )
            assertTrue(
                artifact.coordinate.split(':').size == 3,
                "coordinate is not group:artifact:version for $type",
            )
        }
    }

}

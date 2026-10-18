package top.likoslupus.cellulosesz.economy.worth

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.foundation.database.DeferredDatabaseRuntime

/**
 * Feature repositories are constructed during mod init, before the deferred database runtime is bound
 * at `SERVER_STARTING`. A repository that resolves its SQL dialect in the constructor makes the whole
 * mod fail to start; construction must stay lazy with respect to [DeferredDatabaseRuntime].
 */
class JdbcWorthRepositoryConstructionTest {

    @Test
    fun `construction does not require an initialized database runtime`() {
        val runtime = DeferredDatabaseRuntime()

        assertDoesNotThrow {
            JdbcWorthRepository(runtime, "default")
        }
    }

}

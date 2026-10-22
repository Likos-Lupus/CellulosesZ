package top.likoslupus.cellulosesz.foundation.database.driver

import java.io.PrintWriter
import java.sql.Connection
import java.sql.Driver
import java.sql.SQLException
import java.util.*
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Adapts a driver instance loaded by [IsolatedDriverLoader] to the [DataSource] HikariCP accepts.
 * Using a `DataSource` (instead of `jdbcUrl` + `driverClassName`) keeps the driver out of the mod
 * class loader, which is exactly what makes a downloaded jar usable.
 */
public class DriverDataSource(
    private val driver: Driver,
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
) : DataSource {

    override fun getConnection(): Connection =
        connect(username, password)

    override fun getConnection(username: String?, password: String?): Connection =
        connect(username.orEmpty(), password.orEmpty())

    private fun connect(user: String, secret: String): Connection {
        val properties = Properties()
        if (user.isNotEmpty()) {
            properties.setProperty("user", user)
        }
        if (secret.isNotEmpty()) {
            properties.setProperty("password", secret)
        }
        return driver.connect(jdbcUrl, properties)
            ?: throw SQLException("driver ${driver.javaClass.name} refused $jdbcUrl")
    }

    override fun getLogWriter(): PrintWriter? =
        null

    override fun setLogWriter(out: PrintWriter?) {
        // The driver logs through the pool's logger; a per-data-source writer is not used.
    }

    override fun setLoginTimeout(seconds: Int) {
        // Applied by the pool through driver properties; the driver itself has no global timeout.
    }

    override fun getLoginTimeout(): Int =
        0

    override fun getParentLogger(): Logger =
        Logger.getGlobal()

    override fun <T> unwrap(iface: Class<T>): T {
        if (iface.isInstance(this)) {
            return iface.cast(this)
        }
        throw SQLException("${javaClass.name} is not a wrapper for ${iface.name}")
    }

    override fun isWrapperFor(iface: Class<*>): Boolean =
        iface.isInstance(this)

}

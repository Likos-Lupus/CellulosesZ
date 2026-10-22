package top.likoslupus.cellulosesz.foundation.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the mod obtains the JDBC driver for the active backend. Drivers are never shipped inside the
 * mod jar: the driver is resolved from the mod class loader first, then from [directory], and only
 * downloaded from [repository] when [autoDownload] is on. A driver that cannot be resolved fails
 * the cold start instead of silently degrading to another backend.
 */
@Serializable
public data class DriverSettings(
    @SerialName("auto-download")
    public val autoDownload: Boolean = true,
    @SerialName("directory")
    public val directory: String = "cellulosesz/drivers",
    @SerialName("repository")
    public val repository: String = "https://repo1.maven.org/maven2",
    @SerialName("connect-timeout-ms")
    public val connectTimeoutMs: Long = 10_000,
    @SerialName("read-timeout-ms")
    public val readTimeoutMs: Long = 120_000,
)

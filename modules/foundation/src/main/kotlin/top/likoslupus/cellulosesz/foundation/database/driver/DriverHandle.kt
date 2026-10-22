package top.likoslupus.cellulosesz.foundation.database.driver

import javax.sql.DataSource

/** Where the active driver came from, reported through `/cellulosesz status`. */
public enum class DriverSource {

    CLASSPATH,
    CACHED,
    DOWNLOADED,

}

/** How [top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime] reaches the driver. */
public sealed interface DriverHandle {

    /** The driver is already visible to the mod class loader (a bundled jar or another mod's). */
    public data object Classpath : DriverHandle

    /** The driver was loaded from an external jar; the pool must use this data source. */
    public class External(public val dataSource: DataSource) : DriverHandle

    /** The data source HikariCP must use, or `null` when the driver is on the mod class path. */
    public val externalDataSource: DataSource?
        get() = (this as? External)?.dataSource

}

/** The outcome of provisioning one backend's driver. */
public sealed interface DriverProvision {

    public class Ready(
        public val handle: DriverHandle,
        public val source: DriverSource,
    ) : DriverProvision

    public class Unavailable(
        public val message: String,
    ) : DriverProvision

}

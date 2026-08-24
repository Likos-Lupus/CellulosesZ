package top.likoslupus.cellulosesz.api.teleport

@JvmRecord
public data class RandomTeleportResult(
    public val status: RandomTeleportStatus,
    public val location: CellLocation?
) {

    init {
        require(!(status == RandomTeleportStatus.SUCCESS && location == null)) {
            "successful result requires a location"
        }
    }

    public fun success(): Boolean = status == RandomTeleportStatus.SUCCESS

    public companion object {

        @JvmStatic
        public fun success(location: CellLocation): RandomTeleportResult =
            RandomTeleportResult(RandomTeleportStatus.SUCCESS, location)

        @JvmStatic
        public fun failure(status: RandomTeleportStatus): RandomTeleportResult {
            require(status != RandomTeleportStatus.SUCCESS) { "failure status required" }
            return RandomTeleportResult(status, null)
        }

    }

}

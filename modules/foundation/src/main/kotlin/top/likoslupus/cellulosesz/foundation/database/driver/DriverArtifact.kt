package top.likoslupus.cellulosesz.foundation.database.driver

/**
 * One pinned driver jar: where it comes from inside a Maven repository, and what its bytes must
 * hash to. Instances come from the build-generated manifest (`drivers.properties`), never from
 * network metadata, so a repository cannot substitute a different artifact.
 */
public class DriverArtifact(
    public val coordinate: String,
    public val fileName: String,
    public val size: Long,
    public val sha256: String,
    public val repositoryPath: String,
) {

    override fun toString(): String =
        "DriverArtifact(coordinate=$coordinate, fileName=$fileName, size=$size)"

}

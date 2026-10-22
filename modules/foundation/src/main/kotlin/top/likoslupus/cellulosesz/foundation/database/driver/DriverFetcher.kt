package top.likoslupus.cellulosesz.foundation.database.driver

import top.likoslupus.cellulosesz.foundation.database.DriverSettings
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/**
 * Fetches one pinned driver from the configured Maven repository. The download is blocking because
 * it only runs during the cold-start provisioning step, before the connection pool exists; the
 * stream is verified and moved into the [DriverStore] by the store itself.
 */
public class DriverFetcher(
    private val settings: DriverSettings,
) {

    public fun fetch(artifact: DriverArtifact, store: DriverStore) {
        val url = URI.create(repositoryUrl(artifact)).toURL()
        var lastFailure: IOException? = null
        repeat(ATTEMPTS) {
            try {
                open(url).use { store.install(artifact, it) }
                return
            } catch (exception: IOException) {
                lastFailure = exception
            }
        }
        throw StorageException(
            "could not download ${artifact.coordinate} from $url: ${lastFailure?.message}",
            lastFailure,
        )
    }

    public fun repositoryUrl(artifact: DriverArtifact): String =
        settings.repository.trimEnd('/') + "/" + artifact.repositoryPath

    private fun open(url: java.net.URL): java.io.InputStream =
        (url.openConnection() as? HttpURLConnection)?.let { connection ->
            connection.connectTimeout = settings.connectTimeoutMs.toInt()
            connection.readTimeout = settings.readTimeoutMs.toInt()
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "CellulosesZ")
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IOException("HTTP $status")
            }
            connection.inputStream
        } ?: url.openStream()

    private companion object {

        private const val ATTEMPTS = 2

    }

}

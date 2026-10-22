package top.likoslupus.cellulosesz.foundation.database.driver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.DriverSettings
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

/** Exercises the download path against a local HTTP server; no network is used. */
class DriverFetcherTest {

    @TempDir
    lateinit var tempDir: Path

    private val payload = ByteArray(8192) { (it % 97).toByte() }
    private lateinit var server: HttpServer

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(
            InetSocketAddress("127.0.0.1", 0),
            0
        )
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `a verified download lands in the store exactly once`() {
        serve { exchange -> respond(exchange, 200, payload) }
        val store = DriverStore(tempDir)
        val artifact = artifact()

        DriverFetcher(settings()).fetch(artifact, store)

        val path = store.verified(artifact)
        assertNotNull(path)
        assertArrayEquals(payload, Files.readAllBytes(path!!))
    }

    @Test
    fun `a truncating server is rejected`() {
        serve { exchange ->
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload, 0, payload.size / 3) }
            exchange.close()
        }
        val artifact = artifact()

        val failure = assertThrows(StorageException::class.java) {
            DriverFetcher(settings()).fetch(artifact, DriverStore(tempDir))
        }

        assertTrue(failure.message!!.contains(artifact.coordinate), failure.message)
        assertTrue(Files.notExists(DriverStore(tempDir).expectedPath(artifact)))
    }

    @Test
    fun `a missing artifact reports the status code`() {
        serve { exchange -> respond(exchange, 404, ByteArray(0)) }

        val failure = assertThrows(StorageException::class.java) {
            DriverFetcher(settings()).fetch(artifact(), DriverStore(tempDir))
        }

        assertTrue(failure.message!!.contains("HTTP 404"), failure.message)
    }

    @Test
    fun `a file repository is used without any http`() {
        val payloadPath = tempDir.resolve("mirror/org/example/driver/1.0/driver-1.0.jar")
        Files.createDirectories(payloadPath.parent)
        Files.write(payloadPath, payload)
        val store = DriverStore(tempDir.resolve("drivers"))
        val artifact = artifact()

        DriverFetcher(DriverSettings(repository = tempDir.resolve("mirror").toUri().toString()))
                .fetch(artifact, store)

        assertArrayEquals(
            payload,
            Files.readAllBytes(store.verified(artifact)!!)
        )
    }

    private fun artifact(): DriverArtifact =
        DriverArtifact(
            coordinate = "org.example:driver:1.0",
            fileName = "driver-1.0.jar",
            size = payload.size.toLong(),
            sha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(payload)
            ),
            repositoryPath = "org/example/driver/1.0/driver-1.0.jar",
        )

    private fun settings(): DriverSettings =
        // http is rejected by config validation; the fetcher itself only needs a reachable URL.
        DriverSettings(repository = "http://127.0.0.1:${server.address.port}/repo")

    private fun serve(handler: (HttpExchange) -> Unit) {
        server.createContext("/repo/org/example/driver/1.0/driver-1.0.jar") { exchange ->
            handler(exchange)
        }
        server.start()
    }

    private fun respond(
        exchange: HttpExchange,
        status: Int,
        body: ByteArray
    ) {
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
        exchange.close()
    }

}

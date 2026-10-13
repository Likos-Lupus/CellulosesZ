package top.likoslupus.cellulosesz.communication.page

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.communication.config.ServerPageSettings
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId

/**
 * Policy wrapper around [ServerPageRepository]: it enforces the enabled flag and runs the blocking
 * file read on the IO dispatcher so the server thread is never blocked.
 */
internal class ServerPageService(
    private val repository: ServerPageRepository,
    private val settings: () -> ServerPageSettings,
) {

    suspend fun load(pageName: String, language: LanguageId): ServerPageResult {
        if (!settings().enabled) {
            return ServerPageResult.Disabled
        }
        return withContext(Dispatchers.IO) {
            repository.load(pageName, language)
        }
    }

}

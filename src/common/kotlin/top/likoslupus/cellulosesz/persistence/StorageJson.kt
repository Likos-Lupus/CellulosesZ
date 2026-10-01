package top.likoslupus.cellulosesz.persistence

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

internal object StorageJson {

    @OptIn(ExperimentalSerializationApi::class)
    val format: Json = Json {
        prettyPrint = true
        allowComments = true
        allowTrailingComma = true
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
        isLenient = false
    }

}

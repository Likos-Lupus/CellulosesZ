package top.likoslupus.cellulosesz.foundation.persistence

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/** The single JSON policy used for both config and machine state files. */
public object StorageJson {

    @OptIn(ExperimentalSerializationApi::class)
    public val format: Json = Json {
        prettyPrint = true
        allowComments = true
        allowTrailingComma = true
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
        isLenient = false
    }

}

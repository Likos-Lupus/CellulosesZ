package top.likoslupus.cellulosesz.api.world

import top.likoslupus.cellulosesz.api.validation.requireNonBlank

public sealed interface WorldResolution {

    @JvmRecord
    public data class Resolved(
        public val worldId: String
    ) : WorldResolution {

        init {
            worldId.requireNonBlank { "worldId" }
        }
    }

    public data object NotFound : WorldResolution

    @JvmRecord
    public data class Ambiguous(
        public val candidates: List<String>
    ) : WorldResolution {

        init {
            require(candidates.size >= 2) { "ambiguous resolution needs candidates" }
        }
    }

    public companion object {

        @JvmStatic
        public fun resolved(worldId: String): WorldResolution = Resolved(worldId)

        @JvmStatic
        public fun notFound(): WorldResolution = NotFound

        @JvmStatic
        public fun ambiguous(candidates: List<String>): WorldResolution = Ambiguous(candidates)

    }

}

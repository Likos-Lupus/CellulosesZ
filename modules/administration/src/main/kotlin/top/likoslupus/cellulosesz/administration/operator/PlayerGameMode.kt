package top.likoslupus.cellulosesz.administration.operator

/** The four vanilla game modes CellulosesZ exposes; no Essentials alias expansion. */
internal enum class PlayerGameMode {

    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR;

    companion object {

        fun parse(raw: String): PlayerGameMode? =
            entries.firstOrNull {
                it.name.equals(
                    other = raw,
                    ignoreCase = true
                )
            }

    }

}

package top.likoslupus.cellulosesz.foundation.config

/** Outcome of a transactional config load/reload. On failure the previous snapshot is retained. */
public sealed interface ConfigReloadResult {

    public data class Success(
        public val generation: Int
    ) : ConfigReloadResult

    public data class Failure(
        public val errors: List<ValidationError>
    ) : ConfigReloadResult

}

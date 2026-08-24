package top.likoslupus.cellulosesz.api.command.execution


@JvmRecord
public data class CommandOutcome(
    public val status: Status,
    public val brigadierResult: Int
) {

    public fun successful(): Boolean = status == Status.SUCCESS

    public companion object {

        @JvmStatic
        public fun fromSuccess(success: Boolean): CommandOutcome =
            if (success) {
                success()
            } else {
                rejected()
            }

        @JvmStatic
        public fun success(): CommandOutcome = success(1)

        @JvmStatic
        public fun rejected(): CommandOutcome = rejected(0)

        @JvmStatic
        public fun success(brigadierResult: Int): CommandOutcome =
            CommandOutcome(Status.SUCCESS, brigadierResult)

        @JvmStatic
        public fun rejected(brigadierResult: Int): CommandOutcome =
            CommandOutcome(Status.REJECTED, brigadierResult)

        @JvmStatic
        public fun fromStatus(status: Status): CommandOutcome =
            when (status) {
                Status.SUCCESS -> success()
                Status.REJECTED -> rejected()
                Status.FAILED -> failed()
                Status.PARTIAL -> partial()
            }

        @JvmStatic
        public fun failed(): CommandOutcome = failed(0)

        @JvmStatic
        public fun partial(): CommandOutcome = partial(1)

        @JvmStatic
        public fun failed(brigadierResult: Int): CommandOutcome =
            CommandOutcome(Status.FAILED, brigadierResult)

        @JvmStatic
        public fun partial(brigadierResult: Int): CommandOutcome =
            CommandOutcome(Status.PARTIAL, brigadierResult)

        @JvmStatic
        public fun fromBrigadierResult(result: Int): CommandOutcome =
            if (result > 0) {
                success(result)
            } else {
                rejected(result)
            }

    }

    public enum class Status {

        SUCCESS,
        REJECTED,
        FAILED,
        PARTIAL

    }

}

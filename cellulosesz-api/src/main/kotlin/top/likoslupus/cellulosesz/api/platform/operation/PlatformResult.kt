package top.likoslupus.cellulosesz.api.platform.operation

import top.likoslupus.cellulosesz.api.validation.requireNonBlank

/** Strongly constrained platform operation outcome. */
public class PlatformResult<T> private constructor(
    private val status: PlatformOperationStatus,
    private val rawValue: T?,
    private val detail: String
) {

    init {
        val successful = status == PlatformOperationStatus.SUCCESS ||
                status == PlatformOperationStatus.PARTIAL_SUCCESS
        require(!(!successful && rawValue != null)) { "Failure results cannot carry a value" }
        if (status == PlatformOperationStatus.PARTIAL_SUCCESS) {
            detail.requireNonBlank { "detail" }
        } else require(!(status == PlatformOperationStatus.SUCCESS && detail.isNotEmpty())) {
            "Successful results cannot carry failure detail"
        }
        if (!successful) {
            detail.requireNonBlank { "detail" }
        }
    }

    public fun status(): PlatformOperationStatus = status

    public fun value(): T? = rawValue

    public fun detail(): String = detail

    public fun successful(): Boolean =
        status == PlatformOperationStatus.SUCCESS ||
                status == PlatformOperationStatus.PARTIAL_SUCCESS

    public companion object {

        @JvmStatic
        public fun <T> success(value: T): PlatformResult<T> =
            PlatformResult(
                PlatformOperationStatus.SUCCESS,
                requireNotNull(value) { "value" },
                ""
            )

        @JvmStatic
        public fun success(): PlatformResult<Void> =
            PlatformResult(PlatformOperationStatus.SUCCESS, null, "")

        @JvmStatic
        public fun <T> partial(value: T, detail: String): PlatformResult<T> =
            PlatformResult(
                PlatformOperationStatus.PARTIAL_SUCCESS,
                requireNotNull(value) { "value" },
                detail
            )

        @JvmStatic
        public fun <T> failure(status: PlatformOperationStatus, detail: String): PlatformResult<T> {
            require(
                status != PlatformOperationStatus.SUCCESS &&
                        status != PlatformOperationStatus.PARTIAL_SUCCESS
            ) {
                "Use success or partial factories for successful results"
            }
            return PlatformResult(status, null, detail)
        }

    }

}

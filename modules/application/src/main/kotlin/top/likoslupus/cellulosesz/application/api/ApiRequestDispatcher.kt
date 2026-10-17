package top.likoslupus.cellulosesz.application.api

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/**
 * Runs one asynchronous API request on the kernel's owned coroutine scope and delivers its single
 * completion on the server thread while a session is active. It never uses a raw executor or a
 * blocking wait, and isolates an unexpected failure into a diagnostic
 * [ApiCallResult.Failed] without leaking internal detail.
 */
internal class ApiRequestDispatcher(
    private val kernel: RuntimeKernel,
) {

    private val diagnostics = AtomicLong(0)

    fun <T> submit(
        task: suspend () -> ApiCallResult<T>,
        deliver: (ApiCallResult<T>) -> Unit,
    ): ApiRequestHandle {
        val cancelled = AtomicBoolean(false)
        val completed = AtomicBoolean(false)
        val job: Job? = kernel.launch {
            val result = try {
                task()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                val id = "api-${diagnostics.incrementAndGet()}"
                LOGGER.warn(
                    "api_request_failed diagnostic={} type={}",
                    id,
                    failure.javaClass.simpleName,
                    failure,
                )
                ApiCallResult.Failed(id)
            }
            completed.set(true)
            if (!cancelled.get()) {
                deliverOnServerThread { deliver(result) }
            }
        }

        if (job == null) {
            runCatching { deliver(ApiCallResult.NotReady) }
            return ApiRequestHandle { false }
        }

        return ApiRequestHandle {
            if (completed.get() || !job.isActive) {
                false
            } else {
                cancelled.set(true)
                job.cancel()
                true
            }
        }
    }

    /**
     * The money-request mode: a blocking JDBC commit cannot be interrupted by [Job.cancel], so the
     * task is handed a [MoneyCommitFence] and must call [MoneyCommitFence.tryBeginCommit] immediately
     * before it touches the ledger. Cancellation then succeeds only while the fence is still
     * preparing; afterwards it returns false and the caller recovers the outcome by request key. A
     * cancelled request is never delivered.
     */
    fun <T> submitMoney(
        task: suspend (MoneyCommitFence) -> ApiCallResult<T>,
        deliver: (ApiCallResult<T>) -> Unit,
    ): ApiRequestHandle {
        val fence = MoneyCommitFence()
        val completed = AtomicBoolean(false)
        val job: Job? = kernel.launch {
            val result = try {
                task(fence)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                val id = "api-${diagnostics.incrementAndGet()}"
                LOGGER.warn(
                    "api_request_failed diagnostic={} type={}",
                    id,
                    failure.javaClass.simpleName,
                    failure,
                )
                ApiCallResult.Failed(id)
            } finally {
                fence.finish()
            }
            completed.set(true)
            if (!fence.isCancelled()) {
                deliverOnServerThread { deliver(result) }
            }
        }

        if (job == null) {
            runCatching { deliver(ApiCallResult.NotReady) }
            return ApiRequestHandle { false }
        }

        return ApiRequestHandle {
            !completed.get() &&
                    if (fence.cancelBeforeCommit()) {
                        job.cancel()
                        true
                    } else {
                        false
                    }
        }
    }

    private suspend fun deliverOnServerThread(block: () -> Unit) {
        runCatching { kernel.onServerThread { block() } }
                .onFailure { runCatching { block() } }
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ApiRequestDispatcher::class.java)

    }

}

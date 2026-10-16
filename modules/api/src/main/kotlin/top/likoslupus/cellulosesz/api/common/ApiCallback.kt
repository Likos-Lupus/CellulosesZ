package top.likoslupus.cellulosesz.api.common

/**
 * A Java-callable completion callback. It is invoked **at most once** per accepted request, and never
 * while the API provider holds an internal lock. A thrown exception is isolated and logged; it never
 * changes an already-committed business fact.
 *
 * The callback thread is defined per call site. Domain result callbacks are delivered on the
 * Minecraft server thread while a server session is active; a terminal [ApiCallResult.Stopping]
 * result may be delivered off the server thread once the session has stopped.
 */
public fun interface ApiCallback<T> {

    public fun onComplete(result: ApiCallResult<T>)

}

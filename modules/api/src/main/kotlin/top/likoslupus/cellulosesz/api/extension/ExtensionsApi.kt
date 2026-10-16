package top.likoslupus.cellulosesz.api.extension

/** Read-only diagnostic view of the extensions contributed for the current server session. */
public interface ExtensionsApi {

    /** The addon ids that registered at least one contribution for this session. */
    public fun registeredAddons(): List<String>

}

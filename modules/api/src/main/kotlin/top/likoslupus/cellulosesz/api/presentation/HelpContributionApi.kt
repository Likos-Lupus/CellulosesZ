package top.likoslupus.cellulosesz.api.presentation

import top.likoslupus.cellulosesz.api.actor.AddonAccess

/**
 * A description of an addon-owned command's help page. CellulosesZ only displays it when the command
 * is actually present in the live dispatcher and visible to the executing source; it never registers
 * a command or changes command ownership.
 */
public class HelpEntryDescriptor(
    public val commandName: String,
    public val documentId: String,
    public val category: String,
)

public sealed interface HelpContributionResult {

    public data object Registered : HelpContributionResult

    public data object InvalidKey : HelpContributionResult

    public data object Conflict : HelpContributionResult

    public data object RegistrationClosed : HelpContributionResult

    public data object NotAuthorized : HelpContributionResult

}

/** Contributes help metadata only. Command registration stays with each addon's own Brigadier tree. */
public interface HelpContributionApi {

    public fun register(
        access: AddonAccess,
        entry: HelpEntryDescriptor,
    ): HelpContributionResult

}

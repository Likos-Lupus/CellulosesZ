package top.likoslupus.cellulosesz.api.presentation

/** Groups the Presentation domain: player language, addon notices and help contribution. */
public interface PresentationApi {

    public val language: PlayerLanguageApi

    public val addon: AddonPresentationApi

    public val help: HelpContributionApi

}

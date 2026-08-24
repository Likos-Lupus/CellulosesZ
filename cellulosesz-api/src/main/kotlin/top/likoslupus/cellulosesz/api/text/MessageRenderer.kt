package top.likoslupus.cellulosesz.api.text

public interface MessageRenderer {

    public fun render(
        locale: String,
        key: String
    ): RichText =
        render(
            locale,
            key,
            MessageArguments.empty()
        )

    public fun render(
        locale: String,
        key: String,
        arguments: MessageArguments
    ): RichText

    public fun render(
        locale: String,
        message: LocalizedMessage
    ): RichText =
        render(
            locale,
            message.key,
            message.arguments
        )

    public fun renderInline(
        locale: String,
        template: String
    ): RichText =
        renderInline(
            locale,
            template,
            MessageArguments.empty()
        )

    public fun renderInline(
        locale: String,
        template: String,
        arguments: MessageArguments
    ): RichText

}

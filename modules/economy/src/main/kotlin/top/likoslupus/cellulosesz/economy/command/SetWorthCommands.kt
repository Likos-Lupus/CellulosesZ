package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.item
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.money.MoneyParsing

/** `/setworth` for an explicit item or the item held by the executing player. */
internal object SetWorthCommands {

    fun definition(context: WorthSellContext): CommandDefinition = command(
        name = "setworth",
        category = CommandCategory.ECONOMY,
        permission = CommandPermissions.SET_WORTH,
        documentation = "economy/setworth",
    ) {
        argument("item", item()) { itemKey ->
            argument("price", word()) { price ->
                executes {
                    setWorth(
                        context,
                        source,
                        itemIdOf(get(itemKey)),
                        get(price)
                    )
                }
            }
        }
        argument("price", word()) { price ->
            executesPlayer {
                setWorth(
                    context,
                    source,
                    itemIdOf(source.player),
                    get(price)
                )
            }
        }
    }

    private fun setWorth(
        context: WorthSellContext,
        source: CommandSourceStack,
        itemId: String?,
        priceText: String,
    ): Int {
        val playerId = source.player?.uuid
        val resolved = itemId
            ?: return source.replyError(
                context.adventure,
                context.messages.renderFor(
                    playerId,
                    MessageKey("economy.worth.held.empty")
                ),
            )
        val price = MoneyParsing.parse(priceText)
            ?: return invalidAmount(context, source, playerId)
        val actor = source.player?.gameProfile?.name ?: CONSOLE_ACTOR

        return economyLaunch(
            source,
            context.kernel,
            context.messages,
            context.adventure
        ) { target ->
            val saved = context.worth.set(resolved, price, actor)
            val notice = when {
                saved -> EconomyMessages.setWorth(
                    resolved,
                    price.decimal,
                    context.display()
                )

                else -> EconomyMessages.setWorthFailed()
            }
            context.deliver(target, playerId, notice)
        }
    }

    private const val CONSOLE_ACTOR = "console"

}

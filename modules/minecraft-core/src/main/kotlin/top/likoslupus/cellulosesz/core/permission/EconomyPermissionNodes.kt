package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.adminNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.moderatorNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Economy command nodes, grouped by the bounded context that owns the commands. */
internal object EconomyPermissionNodes {

    internal val BALANCE: PermissionSpec = playerNode(
        "balance",
        "View your balance"
    )

    internal val BALANCE_OTHERS: PermissionSpec = moderatorNode(
        "balance.others",
        "View another player's balance"
    )

    internal val BALANCETOP: PermissionSpec = playerNode(
        "balancetop",
        "View the balance ranking"
    )

    internal val PAY: PermissionSpec = playerNode(
        "pay",
        "Pay another player"
    )

    internal val PAY_TOGGLE: PermissionSpec = playerNode(
        "paytoggle",
        "Toggle accepting payments"
    )

    /** Toggling another player's payment setting; a non-command capability. */
    internal val PAY_TOGGLE_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.paytoggle.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's payment setting"
    )

    /** Paying a player who disabled payments; a non-command capability. */
    internal val PAY_TOGGLE_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.paytoggle.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Pay a player who has disabled payments"
    )

    internal val PAY_CONFIRM_TOGGLE: PermissionSpec = playerNode(
        "payconfirmtoggle",
        "Toggle payment confirmation"
    )

    internal val TRANSACTIONS: PermissionSpec = playerNode(
        "transactions",
        "View your transaction history"
    )

    internal val ECO: PermissionSpec = moderatorNode(
        "eco",
        "Use the economy administration command"
    )

    internal val ECO_GIVE: PermissionSpec = adminNode(
        "eco.give",
        "Grant currency to a player"
    )

    internal val ECO_TAKE: PermissionSpec = adminNode(
        "eco.take",
        "Reclaim currency from a player"
    )

    internal val ECO_SET: PermissionSpec = adminNode(
        "eco.set",
        "Set a player's balance"
    )

    internal val ECO_HISTORY: PermissionSpec = moderatorNode(
        "eco.history",
        "Audit a player's transactions"
    )

    internal val ECO_INFO: PermissionSpec = moderatorNode(
        "eco.info",
        "View economy diagnostics"
    )

    internal val ECONOMY_PAY_CAPABILITY: PermissionSpec = PermissionSpec(
        "cellulosesz.economy.pay",
        VanillaPermissionFallback.ALLOW_ALL,
        "Use the pay capability through integrations"
    )

    internal val WORTH: PermissionSpec = playerNode(
        "worth",
        "Look up an item's worth"
    )

    internal val SELL: PermissionSpec = playerNode(
        "sell",
        "Sell items to the server"
    )

    internal val SET_WORTH: PermissionSpec = moderatorNode(
        "setworth",
        "Set an item's worth"
    )

    internal val SELL_BULK: PermissionSpec = PermissionSpec(
        "cellulosesz.sell.bulk",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Sell an entire inventory in one command"
    )

}

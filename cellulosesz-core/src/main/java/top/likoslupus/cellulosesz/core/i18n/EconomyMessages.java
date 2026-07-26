package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class EconomyMessages {

    private EconomyMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.economy.abstract-economy-command.error.1", "<red>This command can only be used by a player.");
        messages.put("commands.economy.abstract-economy-command.error.2", "<red>Online player not found: <secondary>{value0}<red>");
        messages.put("commands.economy.abstract-economy-command.error.3", "<red>Player not found: <secondary>{value0}<red>");
        messages.put("commands.economy.abstract-economy-command.error.4", "<red>The amount must be greater than 0.");
        messages.put("commands.economy.abstract-economy-command.error.5", "<red>Invalid amount: <secondary>{value0}<red>");
        messages.put("commands.economy.balance-command.reply.1", "<primary>Balance: <secondary>{value0}<primary>");
        messages.put("commands.economy.balance-command.error.1", "<red>You do not have permission to view another player’s balance.");
        messages.put("commands.economy.balance-top-command.error.1", "<red>The page number must be an integer.");
        messages.put("commands.economy.balance-top-command.error.2", "<red>There are no balance entries on this page.");
        messages.put("commands.economy.eco-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.economy.eco-command.error.2", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.economy.pay-command.reply.1", "<primary>Paid <secondary>{value1}<primary> to <secondary>{value0}<primary>. Current balance: <secondary>{value2}<primary>");
        messages.put("commands.economy.pay-received", "<secondary>{player}<primary> paid you <secondary>{amount}<primary>.");
        messages.put("commands.economy.pay-self", "<red>You cannot pay yourself.");
        messages.put("commands.economy.pay-confirm-required", "<primary>Payment of <secondary>{amount}<primary> to <secondary>{player}<primary> requires confirmation. Run <secondary>/pay {player} {amount} {token}<primary> within <secondary>{seconds}<primary> seconds.");
        messages.put("commands.economy.pay-confirm-invalid", "<red>The payment confirmation is invalid, expired, or does not match this payment.");
        messages.put("commands.economy.pay-confirm-unexpected", "<red>This payment does not require a confirmation token.");
        messages.put("commands.economy.pay-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.economy.pay-command.error.2", "<red>The payment amount cannot be less than <secondary>{value0}<red>.");
        messages.put("commands.economy.pay-command.error.3", "<red>That player is not accepting payments.");
        messages.put("commands.economy.set-worth-command.reply.1", "<primary>Set the worth of <secondary>{value0}<primary> to <secondary>{value1}<primary>.");
        messages.put("commands.economy.set-worth-command.error.1", "<red>Invalid amount: <secondary>{value0}<red>");
        messages.put("commands.economy.worth-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.economy.balance-other", "<primary><secondary>{player}<primary>'s balance: <secondary>{balance}");
        messages.put("commands.economy.eco-result", "<primary>{result} Current balance: <secondary>{balance}");
        messages.put("commands.economy.pay-confirm-enabled", "<primary>Large-payment confirmations enabled.");
        messages.put("commands.economy.pay-confirm-disabled", "<primary>Large-payment confirmations disabled.");
        messages.put("commands.economy.payments-enabled", "<primary>Incoming payments enabled.");
        messages.put("commands.economy.payments-disabled", "<primary>Incoming payments disabled.");
        messages.put("commands.economy.worth-removed", "<primary>Removed the worth of <secondary>{item}<primary>.");
        messages.put("commands.economy.worth", "<primary><secondary>{item}<primary> is worth <secondary>{worth}<primary>.");
        messages.put("commands.economy.worth-missing", "<primary>No worth is configured for <secondary>{item}<primary>.");
        messages.put("commands.economy.balance-top", "<primary>Balance top, page <secondary>{page}<primary>:{rows}");
        messages.put("service.economy.negative-amount", "<red>The amount cannot be negative.");
        messages.put("service.economy.balance-maximum", "<red>The balance cannot exceed the configured maximum.");
        messages.put("service.economy.deposit-success", "<primary>Deposit completed.");
        messages.put("service.economy.insufficient-funds", "<red>Insufficient funds.");
        messages.put("service.economy.withdraw-success", "<primary>Withdrawal completed.");
        messages.put("service.economy.balance-out-of-range", "<red>The balance is outside the allowed range.");
        messages.put("service.economy.balance-set", "<primary>Balance set.");
        messages.put("service.economy.self-payment", "<red>You cannot pay yourself.");
        messages.put("service.economy.amount-positive", "<red>The amount must be greater than zero.");
        messages.put("service.economy.recipient-maximum", "<red>The recipient's balance would exceed the maximum.");
        messages.put("service.economy.transfer-success", "<primary>Transfer completed.");
        messages.put("commands.economy.pay-multiple-denied", "<red>You do not have permission to pay multiple players at once.");
        messages.put("commands.economy.pay-too-many", "<red>You may pay at most <secondary>{maximum}<red> players at once.");
        messages.put("commands.economy.pay-offline-denied", "<red>You do not have permission to pay offline player <secondary>{player}<red>.");
        messages.put("commands.economy.pay-ignored", "<red>Payment was blocked because <secondary>{player}<red> is ignoring you.");
        messages.put("service.economy.persistence-failed", "<red>The balance change could not be persisted; no balances were changed.");
        messages.put("commands.economy.balance-top.invalid-filter", "<red>Invalid filter.");
        messages.put("commands.economy.sell.amount-not-allowed-for-all", "<primary>Amount not allowed for all.");
        messages.put("commands.economy.sell.empty-hand", "<red>Empty hand.");
        messages.put("commands.economy.sell.invalid-amount", "<red>Invalid amount.");
        messages.put("commands.economy.sell.invalid-item", "<red>Invalid item.");
        messages.put("commands.economy.sell.inventory-changed", "<primary>Inventory changed.");
        messages.put("commands.economy.component-item-unsupported", "<primary>Items with custom data components cannot be priced or sold by the base-item price table.");
        messages.put("commands.economy.sell.no-sellable-items", "<primary>No sellable items.");
        messages.put("commands.economy.sell.no-worth", "<primary>No worth.");
        messages.put("commands.economy.sell.not-enough", "<red>Only <secondary>{available}<red> of <secondary>{item}<red> are available; <secondary>{requested}<red> requested.");
        messages.put("commands.economy.sell.player-only", "<primary>Player only.");
        messages.put("commands.economy.sell.rollback-failed", "<red>Rollback failed.");
        messages.put("commands.economy.sell.success", "<primary>Sold <secondary>{count}<primary> item(s) for <secondary>{amount}<primary>.");
        messages.put("commands.economy.worth-batch", "<primary>Worth results (<secondary>{found}<primary> found, total <secondary>{total}<primary>):{rows}");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.economy.abstract-economy-command.error.1", "<red>此命令只能由玩家执行。");
        messages.put("commands.economy.abstract-economy-command.error.2", "<red>找不到在线玩家: <secondary>{value0}<red>");
        messages.put("commands.economy.abstract-economy-command.error.3", "<red>找不到玩家: <secondary>{value0}<red>");
        messages.put("commands.economy.abstract-economy-command.error.4", "<red>金额必须大于 0。");
        messages.put("commands.economy.abstract-economy-command.error.5", "<red>金额格式错误: <secondary>{value0}<red>");
        messages.put("commands.economy.balance-command.reply.1", "<primary>余额: <secondary>{value0}<primary>");
        messages.put("commands.economy.balance-command.error.1", "<red>你没有权限查看其他玩家余额。");
        messages.put("commands.economy.balance-top-command.error.1", "<red>页码必须是整数。");
        messages.put("commands.economy.balance-top-command.error.2", "<red>此页没有余额排行记录。");
        messages.put("commands.economy.eco-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.economy.eco-command.error.2", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.economy.pay-command.reply.1", "<primary>已支付 <secondary>{value1}<primary> 给 <secondary>{value0}<primary>。当前余额：<secondary>{value2}<primary>");
        messages.put("commands.economy.pay-received", "<secondary>{player}<primary> 向你支付了 <secondary>{amount}<primary>。");
        messages.put("commands.economy.pay-self", "<red>不能向自己付款。");
        messages.put("commands.economy.pay-confirm-required", "<primary>支付 <secondary>{amount}<primary> 给 <secondary>{player}<primary> 需要确认。请执行 <secondary>/pay {player} {amount} {token}<primary>，并在 <secondary>{seconds}<primary> 秒内完成。");
        messages.put("commands.economy.pay-confirm-invalid", "<red>付款确认无效、已过期或与当前付款不匹配。");
        messages.put("commands.economy.pay-confirm-unexpected", "<red>这笔付款不需要确认令牌。");
        messages.put("commands.economy.pay-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.economy.pay-command.error.2", "<red>付款金额不能低于 <secondary>{value0}<red>");
        messages.put("commands.economy.pay-command.error.3", "<red>该玩家当前不接收付款。");
        messages.put("commands.economy.set-worth-command.reply.1", "<primary>已设置 <secondary>{value0}<primary> 价值为 <secondary>{value1}<primary>。");
        messages.put("commands.economy.set-worth-command.error.1", "<red>金额格式错误: <secondary>{value0}<red>");
        messages.put("commands.economy.worth-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.economy.balance-other", "<primary><secondary>{player}<primary> 的余额：<secondary>{balance}");
        messages.put("commands.economy.eco-result", "<primary>{result} 当前余额：<secondary>{balance}");
        messages.put("commands.economy.pay-confirm-enabled", "<primary>已开启大额付款确认偏好。");
        messages.put("commands.economy.pay-confirm-disabled", "<primary>已关闭大额付款确认偏好。");
        messages.put("commands.economy.payments-enabled", "<primary>已开启收款。");
        messages.put("commands.economy.payments-disabled", "<primary>已关闭收款。");
        messages.put("commands.economy.worth-removed", "<primary>已移除 <secondary>{item}<primary> 的价值。");
        messages.put("commands.economy.worth", "<primary><secondary>{item}<primary> 价值：<secondary>{worth}");
        messages.put("commands.economy.worth-missing", "<primary><secondary>{item}<primary> 没有设置价值。");
        messages.put("commands.economy.balance-top", "<primary>余额排行，第 <secondary>{page}<primary> 页：{rows}");
        messages.put("service.economy.negative-amount", "<red>金额不能为负数。");
        messages.put("service.economy.balance-maximum", "<red>余额不能超过配置上限。");
        messages.put("service.economy.deposit-success", "<primary>存入成功。");
        messages.put("service.economy.insufficient-funds", "<red>余额不足。");
        messages.put("service.economy.withdraw-success", "<primary>扣款成功。");
        messages.put("service.economy.balance-out-of-range", "<red>余额超出允许范围。");
        messages.put("service.economy.balance-set", "<primary>余额已设置。");
        messages.put("service.economy.self-payment", "<red>不能向自己付款。");
        messages.put("service.economy.amount-positive", "<red>金额必须大于 0。");
        messages.put("service.economy.recipient-maximum", "<red>收款方余额会超过上限。");
        messages.put("service.economy.transfer-success", "<primary>转账成功。");
        messages.put("commands.economy.pay-multiple-denied", "<red>你没有权限同时向多名玩家付款。");
        messages.put("commands.economy.pay-too-many", "<red>一次最多可向 <secondary>{maximum}<red> 名玩家付款。");
        messages.put("commands.economy.pay-offline-denied", "<red>你没有权限向离线玩家 <secondary>{player}<red> 付款。");
        messages.put("commands.economy.pay-ignored", "<red><secondary>{player}<red> 已忽略你，付款被阻止。");
        messages.put("service.economy.persistence-failed", "<red>余额变更无法持久化，所有余额均保持不变。");
        messages.put("commands.economy.balance-top.invalid-filter", "<red>操作失败：Invalid filter。");
        messages.put("commands.economy.sell.amount-not-allowed-for-all", "<primary>Amount not allowed for all。");
        messages.put("commands.economy.sell.empty-hand", "<red>操作失败：Empty hand。");
        messages.put("commands.economy.sell.invalid-amount", "<red>操作失败：Invalid amount。");
        messages.put("commands.economy.sell.invalid-item", "<red>操作失败：Invalid item。");
        messages.put("commands.economy.sell.inventory-changed", "<primary>背包内容已变化。");
        messages.put("commands.economy.component-item-unsupported", "<primary>带自定义数据组件的物品不能按基础物品价格表计价或出售。");
        messages.put("commands.economy.sell.no-sellable-items", "<primary>No sellable items。");
        messages.put("commands.economy.sell.no-worth", "<primary>No worth。");
        messages.put("commands.economy.sell.not-enough", "<red>只有 <secondary>{available}<red> 个 <secondary>{item}<red>，请求了 <secondary>{requested}<red> 个。");
        messages.put("commands.economy.sell.player-only", "<primary>Player only。");
        messages.put("commands.economy.sell.rollback-failed", "<red>操作失败：Rollback failed。");
        messages.put("commands.economy.sell.success", "<primary>已出售 <secondary>{count}<primary> 个物品，获得 <secondary>{amount}<primary>。");
        messages.put("commands.economy.worth-batch", "<primary>价值结果（找到 <secondary>{found}<primary> 项，总计 <secondary>{total}<primary>）：{rows}");
        return Map.copyOf(messages);
    }

}

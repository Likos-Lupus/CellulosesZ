package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class TeleportMessages {

    private TeleportMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.teleport.request.accepted-by-target", "<primary>Accepted by target.");
        messages.put("commands.teleport.request.auto-accepted", "<primary>Auto accepted.");
        messages.put("commands.teleport.request.changed", "<primary>Changed.");
        messages.put("commands.teleport.request.denied-by-target", "<red>Denied by target.");
        messages.put("commands.teleport.request.failed", "<red>Failed.");
        messages.put("commands.teleport.request.received-tpa", "<primary>Received tpa.");
        messages.put("commands.teleport.request.received-tpahere", "<primary>Received tpahere.");
        messages.put("commands.teleport.request.unknown-player", "<red>Unknown player.");
        messages.put("commands.teleport.request.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.teleport.settpr.center-player-only", "<primary>Center player only.");
        messages.put("commands.teleport.settpr.current", "<primary>Random teleport for <secondary>{world}<primary>: center <secondary>{centerX}, {centerZ}<primary>, radius <secondary>{minimum}–{maximum}<primary>.");
        messages.put("commands.teleport.settpr.invalid-range", "<red>Invalid range.");
        messages.put("commands.teleport.settpr.updated", "<primary>Updated random teleport for <secondary>{world}<primary>: center <secondary>{centerX}, {centerZ}<primary>, radius <secondary>{minimum}–{maximum}<primary>.");
        messages.put("commands.teleport.settpr.wrong-world", "<primary>Wrong world.");
        messages.put("commands.teleport.tp-auto.disabled", "<red>Disabled.");
        messages.put("commands.teleport.tp-auto.enabled", "<primary>Enabled.");
        messages.put("commands.teleport.tp-auto.invalid-state", "<red>Invalid state.");
        messages.put("commands.teleport.tp-auto.player-only", "<primary>Player only.");
        messages.put("commands.teleport.tpaall.no-targets", "<red>No targets.");
        messages.put("commands.teleport.tpaall.sent", "<primary>Sent requests to <secondary>{count}<primary> player(s).");
        messages.put("commands.teleport.tpall.no-targets", "<red>No targets.");
        messages.put("commands.teleport.tpall.result", "<primary>Teleported <secondary>{success}<primary> of <secondary>{total}<primary> player(s).");
        messages.put("commands.teleport.tpall.target-required", "<primary>Target required.");
        messages.put("commands.teleport.tpoffline.no-location", "<primary>No location.");
        messages.put("commands.teleport.tpoffline.online", "<primary>Online.");
        messages.put("commands.teleport.tpoffline.success", "<primary>Success.");
        messages.put("commands.teleport.tpr-command.success", "<primary>Randomly teleported to <secondary>{location}<primary>.");
        messages.put("commands.teleport.tptoggle.blocks-teleport", "<primary>Blocks teleport.");
        messages.put("commands.teleport.world-command.invalid-world", "<red>Invalid world.");
        messages.put("commands.teleport.world-no-permission", "<primary>World no permission.");
        messages.put("commands.teleport.tpaall.too-many", "<red>There are too many teleport-request targets; the configured maximum is <secondary>{maximum}<red>.");
        messages.put("commands.teleport.tpaall.result", "<primary>Teleport requests: <secondary>{sent}<primary> sent, <secondary>{blocked}<primary> blocked, <secondary>{failed}<primary> failed, <secondary>{total}<primary> considered.");
        messages.put("commands.teleport.tpall.result-detailed", "<primary>Teleport to <secondary>{player}<primary>: <secondary>{success}<primary> succeeded, <secondary>{blocked}<primary> blocked, <secondary>{failed}<primary> failed, <secondary>{total}<primary> considered.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.teleport.request.accepted-by-target", "<primary>操作成功：Accepted by target。");
        messages.put("commands.teleport.request.auto-accepted", "<primary>操作成功：Auto accepted。");
        messages.put("commands.teleport.request.changed", "<primary>Changed。");
        messages.put("commands.teleport.request.denied-by-target", "<red>操作失败：Denied by target。");
        messages.put("commands.teleport.request.failed", "<red>操作失败：Failed。");
        messages.put("commands.teleport.request.received-tpa", "<primary>Received tpa。");
        messages.put("commands.teleport.request.received-tpahere", "<primary>Received tpahere。");
        messages.put("commands.teleport.request.unknown-player", "<red>操作失败：Unknown player。");
        messages.put("commands.teleport.request.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.teleport.settpr.center-player-only", "<primary>操作成功：Center player only。");
        messages.put("commands.teleport.settpr.current", "<primary><secondary>{world}<primary> 的随机传送：中心 <secondary>{centerX}, {centerZ}<primary>，半径 <secondary>{minimum}–{maximum}<primary>。");
        messages.put("commands.teleport.settpr.invalid-range", "<red>操作失败：Invalid range。");
        messages.put("commands.teleport.settpr.updated", "<primary>已更新 <secondary>{world}<primary> 的随机传送：中心 <secondary>{centerX}, {centerZ}<primary>，半径 <secondary>{minimum}–{maximum}<primary>。");
        messages.put("commands.teleport.settpr.wrong-world", "<primary>操作成功：Wrong world。");
        messages.put("commands.teleport.tp-auto.disabled", "<red>操作失败：Disabled。");
        messages.put("commands.teleport.tp-auto.enabled", "<primary>操作成功：Enabled。");
        messages.put("commands.teleport.tp-auto.invalid-state", "<red>操作失败：Invalid state。");
        messages.put("commands.teleport.tp-auto.player-only", "<primary>Player only。");
        messages.put("commands.teleport.tpaall.no-targets", "<red>操作失败：No targets。");
        messages.put("commands.teleport.tpaall.sent", "<primary>已向 <secondary>{count}<primary> 名玩家发送请求。");
        messages.put("commands.teleport.tpall.no-targets", "<red>操作失败：No targets。");
        messages.put("commands.teleport.tpall.result", "<primary>成功传送 <secondary>{success}<primary>/<secondary>{total}<primary> 名玩家。");
        messages.put("commands.teleport.tpall.target-required", "<primary>Target required。");
        messages.put("commands.teleport.tpoffline.no-location", "<primary>No location。");
        messages.put("commands.teleport.tpoffline.online", "<primary>Online。");
        messages.put("commands.teleport.tpoffline.success", "<primary>操作成功：Success。");
        messages.put("commands.teleport.tpr-command.success", "<primary>已随机传送到 <secondary>{location}<primary>。");
        messages.put("commands.teleport.tptoggle.blocks-teleport", "<primary>Blocks teleport。");
        messages.put("commands.teleport.world-command.invalid-world", "<red>操作失败：Invalid world。");
        messages.put("commands.teleport.world-no-permission", "<primary>World no permission。");
        messages.put("commands.teleport.tpaall.too-many", "<red>传送请求目标过多；配置上限为 <secondary>{maximum}<red> 名玩家。");
        messages.put("commands.teleport.tpaall.result", "<primary>传送请求结果：发送 <secondary>{sent}<primary>，阻止 <secondary>{blocked}<primary>，失败 <secondary>{failed}<primary>，共处理 <secondary>{total}<primary>。");
        messages.put("commands.teleport.tpall.result-detailed", "<primary>传送到 <secondary>{player}<primary>：成功 <secondary>{success}<primary>，阻止 <secondary>{blocked}<primary>，失败 <secondary>{failed}<primary>，共处理 <secondary>{total}<primary>。");
        return Map.copyOf(messages);
    }

}

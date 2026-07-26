package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class PlayerStateMessages {

    private PlayerStateMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.playerstate.afk-kicked", "<primary>Afk kicked.");
        messages.put("commands.playerstate.gamemode-invalid", "<red>Gamemode invalid.");
        messages.put("commands.playerstate.gamemode-set", "<primary>Gamemode set.");
        messages.put("commands.playerstate.gamemode-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.playerstate.near-empty", "<red>Near empty.");
        messages.put("commands.playerstate.near-invalid-radius", "<red>Near invalid radius.");
        messages.put("commands.playerstate.near-list", "<primary>Visible players within <secondary>{radius}<primary> blocks: {entries}");
        messages.put("commands.playerstate.near-radius-range", "<primary>Near radius range.");
        messages.put("commands.playerstate.playtime", "<primary><secondary>{player}<primary> has played for <secondary>{playtime}<primary>.");
        messages.put("commands.playerstate.ptime-failed", "<red>Ptime failed.");
        messages.put("commands.playerstate.ptime-invalid", "<red>Ptime invalid.");
        messages.put("commands.playerstate.ptime-reset", "<primary>Ptime reset.");
        messages.put("commands.playerstate.ptime-set", "<primary>Ptime set.");
        messages.put("commands.playerstate.ptime-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.playerstate.pweather-failed", "<red>Pweather failed.");
        messages.put("commands.playerstate.pweather-invalid", "<red>Pweather invalid.");
        messages.put("commands.playerstate.pweather-reset", "<primary>Pweather reset.");
        messages.put("commands.playerstate.pweather-set", "<primary>Pweather set.");
        messages.put("commands.playerstate.pweather-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.playerstate.seen-never", "<primary>Seen never.");
        messages.put("commands.playerstate.seen-offline", "<primary>Seen offline.");
        messages.put("commands.playerstate.seen-online", "<primary>Seen online.");
        messages.put("commands.playerstate.seen-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.playerstate.speed-failed", "<red>Speed failed.");
        messages.put("commands.playerstate.speed-invalid", "<red>Speed invalid.");
        messages.put("commands.playerstate.speed-set", "<primary>Speed set.");
        messages.put("commands.playerstate.speed-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.playerstate.whois", "<primary>{player}: UUID=<secondary>{uuid}<primary>, online=<secondary>{online}<primary>, AFK=<secondary>{afk}<primary>, first=<secondary>{firstJoin}<primary>, last join=<secondary>{lastJoin}<primary>, last quit=<secondary>{lastQuit}<primary>, playtime=<secondary>{playtime}<primary>, nickname=<secondary>{nickname}<primary>.");
        messages.put("commands.playerstate.whois-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.playerstate.playtime-invalid", "<red>The stored playtime is invalid.");
        messages.put("commands.playerstate.ptime-rollback-failed", "<red>Personal time could not be applied and the stored preference could not be rolled back.");
        messages.put("commands.playerstate.pweather-rollback-failed", "<red>Personal weather could not be applied and the stored preference could not be rolled back.");
        messages.put("service.playerstate.vanish-failed", "<red>Vanish state could not be applied.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.playerstate.afk-kicked", "<primary>Afk kicked。");
        messages.put("commands.playerstate.gamemode-invalid", "<red>操作失败：Gamemode invalid。");
        messages.put("commands.playerstate.gamemode-set", "<primary>操作成功：Gamemode set。");
        messages.put("commands.playerstate.gamemode-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.playerstate.near-empty", "<red>操作失败：Near empty。");
        messages.put("commands.playerstate.near-invalid-radius", "<red>操作失败：Near invalid radius。");
        messages.put("commands.playerstate.near-list", "<primary><secondary>{radius}<primary> 格内的可见玩家：{entries}");
        messages.put("commands.playerstate.near-radius-range", "<primary>Near radius range。");
        messages.put("commands.playerstate.playtime", "<primary><secondary>{player}<primary> 的游戏时间为 <secondary>{playtime}<primary>。");
        messages.put("commands.playerstate.ptime-failed", "<red>操作失败：Ptime failed。");
        messages.put("commands.playerstate.ptime-invalid", "<red>操作失败：Ptime invalid。");
        messages.put("commands.playerstate.ptime-reset", "<primary>操作成功：Ptime reset。");
        messages.put("commands.playerstate.ptime-set", "<primary>操作成功：Ptime set。");
        messages.put("commands.playerstate.ptime-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.playerstate.pweather-failed", "<red>操作失败：Pweather failed。");
        messages.put("commands.playerstate.pweather-invalid", "<red>操作失败：Pweather invalid。");
        messages.put("commands.playerstate.pweather-reset", "<primary>操作成功：Pweather reset。");
        messages.put("commands.playerstate.pweather-set", "<primary>操作成功：Pweather set。");
        messages.put("commands.playerstate.pweather-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.playerstate.seen-never", "<primary>Seen never。");
        messages.put("commands.playerstate.seen-offline", "<primary>Seen offline。");
        messages.put("commands.playerstate.seen-online", "<primary>Seen online。");
        messages.put("commands.playerstate.seen-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.playerstate.speed-failed", "<red>操作失败：Speed failed。");
        messages.put("commands.playerstate.speed-invalid", "<red>操作失败：Speed invalid。");
        messages.put("commands.playerstate.speed-set", "<primary>操作成功：Speed set。");
        messages.put("commands.playerstate.speed-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.playerstate.whois", "<primary>{player}：UUID=<secondary>{uuid}<primary>，在线=<secondary>{online}<primary>，AFK=<secondary>{afk}<primary>，首次加入=<secondary>{firstJoin}<primary>，最近加入=<secondary>{lastJoin}<primary>，最近退出=<secondary>{lastQuit}<primary>，游戏时间=<secondary>{playtime}<primary>，昵称=<secondary>{nickname}<primary>。");
        messages.put("commands.playerstate.whois-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.playerstate.playtime-invalid", "<red>保存的游戏时长无效。");
        messages.put("commands.playerstate.ptime-rollback-failed", "<red>无法应用个人时间，且保存的偏好未能回滚。");
        messages.put("commands.playerstate.pweather-rollback-failed", "<red>无法应用个人天气，且保存的偏好未能回滚。");
        messages.put("service.playerstate.vanish-failed", "<red>无法应用隐身状态。");
        return Map.copyOf(messages);
    }

}

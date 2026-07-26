package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class PlayerStateMessages {

    private PlayerStateMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.playerstate.abstract-player-state-command.error.1", "<red>You do not have permission to affect another player.");
        messages.put("commands.playerstate.abstract-player-state-command.error.2", "<red>Online player not found: <secondary>{value0}<red>");
        messages.put("commands.playerstate.abstract-player-state-command.error.3", "<red>This command can only be used by a player.");
        messages.put("commands.playerstate.nick-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.playerstate.vanish-command.error.1", "<red>You do not have permission to change another player’s vanish state.");
        messages.put("commands.playerstate.vanish-command.error.2", "<red>Player is not online: <secondary>{value0}<red>");
        messages.put("commands.playerstate.vanish-command.error.3", "<red>Usage: <secondary>{value0}<red>");
        messages.put("service.playerstate.fly-failed", "<red>Unable to change the flying state.");
        messages.put("service.playerstate.fly-enabled", "<primary>Enabled flying for <secondary>{player}<primary>.");
        messages.put("service.playerstate.fly-disabled", "<primary>Disabled flying for <secondary>{player}<primary>.");
        messages.put("service.playerstate.god-failed", "<red>Unable to change invulnerability.");
        messages.put("service.playerstate.god-enabled", "<primary>Enabled god mode for <secondary>{player}<primary>.");
        messages.put("service.playerstate.god-disabled", "<primary>Disabled god mode for <secondary>{player}<primary>.");
        messages.put("service.playerstate.heal-success", "<primary>Healed <secondary>{player}<primary>.");
        messages.put("service.playerstate.heal-failed", "<red>Failed to heal <secondary>{player}<red>.");
        messages.put("service.playerstate.feed-success", "<primary>Fed <secondary>{player}<primary>.");
        messages.put("service.playerstate.feed-failed", "<red>Failed to feed <secondary>{player}<red>.");
        messages.put("service.playerstate.afk-enabled", "<secondary>{player}<primary> is now AFK.");
        messages.put("service.playerstate.afk-disabled", "<secondary>{player}<primary> is no longer AFK.");
        messages.put("service.playerstate.user-not-loaded", "<red>Player data is not loaded: <secondary>{player}<red>");
        messages.put("service.playerstate.vanish-enabled", "<primary>Vanished <secondary>{player}<primary>.");
        messages.put("service.playerstate.vanish-disabled", "<primary>Unvanished <secondary>{player}<primary>.");
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
        messages.put("player.list", "<primary>Online players (<secondary>{count}<primary>): {players}");
        messages.put("player.nick-set", "<primary>Nickname set to <secondary>{nickname}<primary>.");
        messages.put("player.nick-cleared", "<primary>Nickname cleared.");
        messages.put("player.nick-invalid", "<red>The nickname does not match the configured rules.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.playerstate.abstract-player-state-command.error.1", "<red>你没有权限操作其他玩家。");
        messages.put("commands.playerstate.abstract-player-state-command.error.2", "<red>找不到在线玩家: <secondary>{value0}<red>");
        messages.put("commands.playerstate.abstract-player-state-command.error.3", "<red>此命令只能由玩家执行。");
        messages.put("commands.playerstate.nick-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.playerstate.vanish-command.error.1", "<red>你没有权限修改其他玩家的隐身状态。");
        messages.put("commands.playerstate.vanish-command.error.2", "<red>玩家不在线: <secondary>{value0}<red>");
        messages.put("commands.playerstate.vanish-command.error.3", "<red>用法: <secondary>{value0}<red>");
        messages.put("service.playerstate.fly-failed", "<red>无法修改飞行状态。");
        messages.put("service.playerstate.fly-enabled", "<primary>已为 <secondary>{player}<primary> 开启飞行。");
        messages.put("service.playerstate.fly-disabled", "<primary>已为 <secondary>{player}<primary> 关闭飞行。");
        messages.put("service.playerstate.god-failed", "<red>无法修改无敌状态。");
        messages.put("service.playerstate.god-enabled", "<primary>已为 <secondary>{player}<primary> 开启无敌。");
        messages.put("service.playerstate.god-disabled", "<primary>已为 <secondary>{player}<primary> 关闭无敌。");
        messages.put("service.playerstate.heal-success", "<primary>已治疗 <secondary>{player}<primary>。");
        messages.put("service.playerstate.heal-failed", "<red>治疗 <secondary>{player}<red> 失败。");
        messages.put("service.playerstate.feed-success", "<primary>已喂饱 <secondary>{player}<primary>。");
        messages.put("service.playerstate.feed-failed", "<red>喂食 <secondary>{player}<red> 失败。");
        messages.put("service.playerstate.afk-enabled", "<secondary>{player}<primary> 现在离开。");
        messages.put("service.playerstate.afk-disabled", "<secondary>{player}<primary> 回来了。");
        messages.put("service.playerstate.user-not-loaded", "<red>玩家数据尚未加载：<secondary>{player}<red>");
        messages.put("service.playerstate.vanish-enabled", "<primary>已使 <secondary>{player}<primary> 隐身。");
        messages.put("service.playerstate.vanish-disabled", "<primary>已使 <secondary>{player}<primary> 显身。");
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
        messages.put("player.list", "<primary>在线玩家（<secondary>{count}<primary>）：{players}");
        messages.put("player.nick-set", "<primary>昵称已设置为 <secondary>{nickname}<primary>。");
        messages.put("player.nick-cleared", "<primary>昵称已清除。");
        messages.put("player.nick-invalid", "<red>昵称不符合配置规则。");
        return Map.copyOf(messages);
    }

}

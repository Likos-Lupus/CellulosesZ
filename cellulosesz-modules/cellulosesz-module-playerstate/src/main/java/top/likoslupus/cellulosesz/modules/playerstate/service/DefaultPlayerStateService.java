package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Optional;
import java.util.UUID;

public final class DefaultPlayerStateService implements PlayerStateService {

    private final PlatformService platform;
    private final UserService users;

    public DefaultPlayerStateService(
            PlatformService platform,
            UserService users
    ) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public AdminResult setFlying(CellPlayer player, boolean enabled) {
        if (!platform.setFlying(player, enabled)) return AdminResult.failure("无法修改飞行状态。");
        users.cached(player.uuid()).ifPresent(user -> {
            user.state.flying = enabled;
            users.markDirty(player.uuid());
        });
        return AdminResult.success((enabled ? "已开启" : "已关闭") + "飞行: " + player.name());
    }

    @Override
    public AdminResult setGod(CellPlayer player, boolean enabled) {
        if (!platform.setInvulnerable(player, enabled)) return AdminResult.failure("无法修改无敌状态。");
        users.cached(player.uuid()).ifPresent(user -> {
            user.state.god = enabled;
            users.markDirty(player.uuid());
        });
        return AdminResult.success((enabled ? "已开启" : "已关闭") + "无敌: " + player.name());
    }

    @Override
    public AdminResult heal(CellPlayer player) {
        return platform.heal(player)
                ? AdminResult.success("已治疗 " + player.name() + "。")
                : AdminResult.failure("治疗失败: " + player.name());
    }

    @Override
    public AdminResult feed(CellPlayer player) {
        return platform.feed(player)
                ? AdminResult.success("已喂饱 " + player.name() + "。")
                : AdminResult.failure("喂食失败: " + player.name());
    }

    @Override
    public AdminResult setAfk(
            UUID uuid,
            String name,
            boolean afk
    ) {
        users.cached(uuid).ifPresent(user -> {
            user.state.afk = afk;
            users.markDirty(uuid);
        });
        return AdminResult.success("%s%s".formatted(name, afk ? " 现在离开。" : " 回来了。"));
    }

    @Override
    public boolean afk(UUID uuid) {
        return users.cached(uuid)
                .map(user -> user.state.afk)
                .orElse(false);
    }

    @Override
    public AdminResult setNick(
            UUID uuid,
            String name,
            Optional<String> nickname
    ) {
        users.cached(uuid).ifPresent(user -> {
            user.state.nickname = nickname
                    .filter(value -> !value.isBlank())
                    .orElse(null);
            users.markDirty(uuid);
        });
        return AdminResult.success(nickname.filter(value -> !value.isBlank())
                .map(value -> "已将 %s 的昵称设为 %s。".formatted(name, value))
                .orElse("已清除 %s 的昵称。".formatted(name)));
    }

    @Override
    public Optional<String> nick(UUID uuid) {
        return users.cached(uuid)
                .flatMap(user -> Optional.ofNullable(user.state.nickname));
    }

}

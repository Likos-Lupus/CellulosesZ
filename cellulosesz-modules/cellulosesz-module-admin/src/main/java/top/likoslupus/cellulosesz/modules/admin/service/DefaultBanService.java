package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

public final class DefaultBanService implements BanService {

    private final PlatformService platform;

    public DefaultBanService(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public AdminResult ban(
            String target,
            String actor,
            String reason
    ) {
        return command(
                "ban %s%s".formatted(target, suffix(reason)),
                "已封禁 %s。".formatted(target),
                "封禁失败: %s".formatted(target)
        );
    }

    @Override
    public AdminResult unban(String target, String actor) {
        return command(
                "pardon %s".formatted(target),
                "已解除封禁 %s。".formatted(target),
                "解除封禁失败: %s".formatted(target)
        );
    }

    @Override
    public AdminResult banIp(
            String target,
            String actor,
            String reason
    ) {
        return command(
                "ban-ip %s%s".formatted(target, suffix(reason)),
                "已封禁 IP %s。".formatted(target),
                "IP 封禁失败: %s".formatted(target)
        );
    }

    @Override
    public AdminResult unbanIp(String target, String actor) {
        return command(
                "pardon-ip %s".formatted(target),
                "已解除 IP 封禁 %s。".formatted(target),
                "解除 IP 封禁失败: %s".formatted(target)
        );
    }

    @Override
    public AdminResult kick(
            String target,
            String actor,
            String reason
    ) {
        var player = platform.onlinePlayer(target);
        if (player.isPresent()) {
            platform.kick(player.get(), reason.isBlank() ? "Kicked by an operator." : reason);
            return AdminResult.success("已踢出 %s。".formatted(player.get().name()));
        }

        return command(
                "kick %s%s".formatted(target, suffix(reason)),
                "已踢出 %s。".formatted(target),
                "踢出失败: %s".formatted(target)
        );
    }

    @Override
    public AdminResult kickAll(String actor, String reason) {
        var message = reason.isBlank() ? "Kicked by an operator." : reason;
        var count = platform.onlinePlayers().stream()
                .mapToInt(player -> {
                    platform.kick(player, message);
                    return 1;
                })
                .sum();
        return AdminResult.success("已踢出 %d 名玩家。".formatted(count));
    }

    private AdminResult command(
            String command,
            String success,
            String failure
    ) {
        return platform.dispatchConsoleCommand(command)
                ? AdminResult.success(success)
                : AdminResult.failure(failure);
    }

    private String suffix(String reason) {
        return reason.isBlank() ? "" : " " + reason;
    }

}

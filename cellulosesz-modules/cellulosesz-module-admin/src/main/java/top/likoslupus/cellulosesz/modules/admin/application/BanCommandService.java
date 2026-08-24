package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargetInput;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;

import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface BanCommandService {

    CompletableFuture<AdminResult> ban(
            String player,
            AdminActor actor,
            String reason
    );

    CompletableFuture<AdminResult> unban(String player, AdminActor actor);

    CompletableFuture<AdminResult> banIp(
            NetworkTargetInput target,
            AdminActor actor,
            String reason
    );

    CompletableFuture<AdminResult> unbanIp(InetAddress address, AdminActor actor);

    CompletableFuture<AdminResult> tempBan(
            String player,
            AdminActor actor,
            Duration duration,
            String reason
    );

    CompletableFuture<AdminResult> tempBanIp(
            NetworkTargetInput target,
            AdminActor actor,
            Duration duration,
            String reason
    );

}

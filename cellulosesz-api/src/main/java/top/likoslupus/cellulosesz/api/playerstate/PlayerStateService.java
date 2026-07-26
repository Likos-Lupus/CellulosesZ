package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerStateService {

    CompletableFuture<AdminResult> setFlying(CellPlayer player, boolean enabled);

    CompletableFuture<AdminResult> setGod(CellPlayer player, boolean enabled);

    AdminResult heal(CellPlayer player);

    AdminResult feed(CellPlayer player);

    CompletableFuture<AdminResult> setAfk(
            UUID uuid,
            String name,
            boolean afk
    );

    boolean afk(UUID uuid);

    void activity(UUID uuid, long timestamp);

    long lastActivity(UUID uuid);

    long idleMillis(UUID uuid);

    CompletableFuture<AdminResult> setNick(
            UUID uuid,
            String name,
            Optional<String> nickname
    );

    Optional<String> nick(UUID uuid);

}

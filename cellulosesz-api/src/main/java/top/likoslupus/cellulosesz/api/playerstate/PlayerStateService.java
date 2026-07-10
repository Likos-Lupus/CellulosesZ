package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

public interface PlayerStateService {

    AdminResult setFlying(CellPlayer player, boolean enabled);

    AdminResult setGod(CellPlayer player, boolean enabled);

    AdminResult heal(CellPlayer player);

    AdminResult feed(CellPlayer player);

    AdminResult setAfk(
            UUID uuid,
            String name,
            boolean afk
    );

    boolean afk(UUID uuid);

    AdminResult setNick(
            UUID uuid,
            String name,
            Optional<String> nickname
    );

    Optional<String> nick(UUID uuid);

}

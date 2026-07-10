package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface JailService {

    AdminResult setJail(
            String name,
            CellLocation location,
            String actor
    );

    AdminResult deleteJail(String name);

    Optional<Jail> jail(String name);

    Collection<Jail> jails();

    AdminResult jailPlayer(
            CellPlayer player,
            String jail,
            String actor,
            Long durationMillis,
            String reason
    );

    AdminResult unjail(
            UUID uuid,
            String name,
            String actor
    );

    Optional<JailedPlayer> jailed(UUID uuid);

    Collection<JailedPlayer> jailedPlayers();

    void purgeExpired();

}

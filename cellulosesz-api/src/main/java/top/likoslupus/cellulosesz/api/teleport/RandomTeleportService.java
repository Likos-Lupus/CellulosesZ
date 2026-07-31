package top.likoslupus.cellulosesz.api.teleport;

public interface RandomTeleportService {

    RandomTeleportResult randomLocation(String world, RandomTeleportSettings settings);

}

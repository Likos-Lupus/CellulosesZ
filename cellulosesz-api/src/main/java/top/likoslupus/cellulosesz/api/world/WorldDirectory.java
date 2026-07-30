package top.likoslupus.cellulosesz.api.world;

import java.util.List;
import java.util.Optional;

public interface WorldDirectory {

    List<String> loadedWorldIds();

    default Optional<String> resolveLoadedWorld(String input) {
        return resolve(input).worldId();
    }

    WorldResolution resolve(String input);

}

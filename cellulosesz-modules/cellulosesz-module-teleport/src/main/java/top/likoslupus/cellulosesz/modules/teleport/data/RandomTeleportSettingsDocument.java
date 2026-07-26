package top.likoslupus.cellulosesz.modules.teleport.data;

import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettings;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RandomTeleportSettingsDocument {

    public Map<String, RandomTeleportSettings> worlds = new LinkedHashMap<>();

}

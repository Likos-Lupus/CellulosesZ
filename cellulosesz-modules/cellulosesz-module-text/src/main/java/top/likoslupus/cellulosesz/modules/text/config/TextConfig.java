package top.likoslupus.cellulosesz.modules.text.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TextConfig {

    public int pageSize = 8;
    public boolean showMotdOnJoin = true;
    public List<String> info = List.of("CellulosesZ server utilities.");
    public List<String> motd = List.of("Welcome to the server.");
    public List<String> rules = List.of("Respect other players.");
    public Map<String, List<String>> custom = new LinkedHashMap<>();

}

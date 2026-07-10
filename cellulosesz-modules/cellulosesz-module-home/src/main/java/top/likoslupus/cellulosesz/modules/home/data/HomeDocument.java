package top.likoslupus.cellulosesz.modules.home.data;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HomeDocument {

    public int schema = 1;
    public UUID uuid = new UUID(0L, 0L);
    public Map<String, CellLocation> homes = new LinkedHashMap<>();

    public HomeDocument() {
    }

    public HomeDocument(UUID uuid) {
        this.uuid = uuid;
    }

}

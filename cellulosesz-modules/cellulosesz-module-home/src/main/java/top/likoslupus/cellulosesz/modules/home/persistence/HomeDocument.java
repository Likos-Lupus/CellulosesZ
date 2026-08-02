package top.likoslupus.cellulosesz.modules.home.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Mutable persisted home collection. */
public final class HomeDocument {

    public @Nullable String uuid;
    public Map<String, LocationDocument> homes = new LinkedHashMap<>();

}

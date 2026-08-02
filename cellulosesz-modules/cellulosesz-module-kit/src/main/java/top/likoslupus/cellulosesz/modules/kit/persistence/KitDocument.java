package top.likoslupus.cellulosesz.modules.kit.persistence;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Mutable YAML representation of a kit definition. */
public final class KitDocument {

    public @Nullable String id;
    public @Nullable String displayName;
    public @Nullable String permission = "";
    public long cooldownSeconds;
    public @Nullable String cost = "0";
    public List<KitItemDocument> items = new ArrayList<>();

}

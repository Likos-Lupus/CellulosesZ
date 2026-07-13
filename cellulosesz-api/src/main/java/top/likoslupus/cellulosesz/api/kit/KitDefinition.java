package top.likoslupus.cellulosesz.api.kit;

import java.util.ArrayList;
import java.util.List;

public final class KitDefinition {

    public String id = "starter";
    public String displayName = "Starter";
    public String permission = "";
    public long cooldownSeconds = 0L;
    public String cost = "0.00";
    public List<KitItem> items = new ArrayList<>();

}

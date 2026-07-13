package top.likoslupus.cellulosesz.api.warp;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.UUID;

public final class Warp {

    public String name = "";
    public String displayName = "";
    public String cost = "0.00";
    public CellLocation location = new CellLocation();
    public @Nullable UUID createdBy;
    public long createdAt;

    public Warp() {
    }

    public Warp(
            String name,
            CellLocation location
    ) {
        this.name = name;
        this.displayName = name;
        this.location = location;
        this.createdAt = System.currentTimeMillis();
    }

}

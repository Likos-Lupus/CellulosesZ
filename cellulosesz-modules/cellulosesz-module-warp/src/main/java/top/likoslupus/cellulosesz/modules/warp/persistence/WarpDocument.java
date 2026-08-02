package top.likoslupus.cellulosesz.modules.warp.persistence;

import org.jspecify.annotations.Nullable;

/** Mutable JSON representation of a warp. */
public final class WarpDocument {

    public @Nullable String name;
    public @Nullable String displayName;
    public @Nullable String cost = "0.00";
    public LocationDocument location = new LocationDocument();
    public @Nullable String createdBy;
    public long createdAt;

}

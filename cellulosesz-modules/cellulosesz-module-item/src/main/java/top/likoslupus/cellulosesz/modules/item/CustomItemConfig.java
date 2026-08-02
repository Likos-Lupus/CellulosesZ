package top.likoslupus.cellulosesz.modules.item;

import org.jspecify.annotations.Nullable;

/** Mutable configuration shape mapped to an immutable item descriptor after loading. */
public final class CustomItemConfig {

    public @Nullable String item;
    public int count = 1;
    public @Nullable String argument;

}

package top.likoslupus.cellulosesz.core.command;

import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Canonical direct-command roots that must never be routed through the legacy spec bridge.
 */
public final class DirectCommandMigrationIndex {

    private static final Set<String> ROOTS = Set.of(
            "afk", "balance", "balancetop", "broadcast", "broadcastworld", "cellulosesz",
            "compass", "createkit", "customtext", "delhome", "delkit", "delwarp", "depth",
            "eco", "exp", "feed", "fly", "gamemode", "getpos", "god", "heal", "help",
            "helpop", "home", "ignore", "info", "kit", "kitreset", "list", "mail", "me",
            "motd", "msg", "msgtoggle", "near", "nick", "pay", "payconfirmtoggle", "paytoggle",
            "ping", "playtime", "ptime", "pweather", "r", "realname", "renamehome", "rest",
            "rtoggle", "rules", "seen", "sell", "sethome", "setwarp", "setworth", "showkit",
            "socialspy", "speed", "vanish", "warp", "warpinfo", "whois", "worth"
    );

    private DirectCommandMigrationIndex() {
    }

    public static boolean contains(String canonicalRoot) {
        return ROOTS.contains(requireNonNull(canonicalRoot, "canonicalRoot").toLowerCase(Locale.ROOT));
    }

    public static Set<String> roots() {
        return ROOTS;
    }

}

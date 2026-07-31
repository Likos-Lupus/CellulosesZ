package top.likoslupus.cellulosesz.core.command;

import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Canonical direct-command roots that must never be routed through the legacy spec bridge.
 */
public final class DirectCommandMigrationIndex {

    private static final Set<String> ROOTS = Set.of(
            "afk", "back", "balance", "balancetop", "ban", "banip", "bottom", "broadcast",
            "broadcastworld", "burn", "cellulosesz", "compass", "createkit", "customtext", "delhome",
            "deljail", "delkit", "delwarp", "depth", "eco", "exp", "ext", "feed", "fly", "gamemode",
            "getpos", "god", "heal", "help", "helpop", "home", "ice", "ignore", "info", "jail",
            "jailedplayers", "jails", "jump", "kick", "kickall", "kill", "kit", "kitreset", "list", "mail",
            "me", "motd", "msg", "msgtoggle", "mute", "near", "nick", "pay", "payconfirmtoggle",
            "paytoggle", "ping", "playtime", "ptime", "pweather", "r", "realname", "renamehome", "rest",
            "rtoggle", "rules", "seen", "sell", "sethome", "setjail", "settpr", "setwarp", "setworth",
            "showkit", "socialspy", "speed", "sudo", "suicide", "tempban", "tempbanip", "top", "tp", "tpa",
            "tpaall", "tpacancel", "tpaccept", "tpahere", "tpall", "tpauto", "tpdeny", "tphere", "tpo",
            "tpoffline", "tpohere", "tppos", "tpr", "tptoggle", "unban", "unbanip", "vanish", "warp",
            "warpinfo", "whois", "world", "worth"
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

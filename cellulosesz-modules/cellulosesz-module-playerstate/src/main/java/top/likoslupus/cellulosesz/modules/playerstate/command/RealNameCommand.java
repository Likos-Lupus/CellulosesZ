package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class RealNameCommand implements CellCommand {

    private static final int MAX_RESULTS = 20;
    private static final Pattern LEGACY = Pattern.compile("(?i)[§&][0-9A-FK-ORX]");
    private static final Pattern MINI_TAG = Pattern.compile("<[^>]{1,64}>");
    private final PlatformService platform;
    private final DisplayNameService displayNames;
    private final VanishService vanish;

    public RealNameCommand(
            PlatformService platform,
            DisplayNameService displayNames,
            VanishService vanish
    ) {
        this.platform = platform;
        this.displayNames = displayNames;
        this.vanish = vanish;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.realname";
    }

    @Override
    public String usage() {
        return "/realname <nickname>";
    }

    @Override
    public String name() {
        return "realname";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey("commands.playerstate.realname.usage", Map.of("usage", usage()));
            return 0;
        }
        var query = normalize(invocation.args()[0]);
        if (query.isEmpty()) {
            invocation.errorKey("commands.playerstate.realname.invalid-query");
            return 0;
        }
        var viewer = platform.player(invocation);
        var matches = platform.onlinePlayers().stream()
                .filter(target -> viewer.isEmpty() || vanish.canSee(viewer.orElseThrow(), target.uuid()))
                .filter(target -> normalize(displayNames.plainDisplayName(target)).contains(query))
                .sorted(java.util.Comparator.comparing(target -> target.name().toLowerCase(Locale.ROOT)))
                .limit(MAX_RESULTS)
                .toList();
        if (matches.isEmpty()) {
            invocation.errorKey("commands.playerstate.realname.none", Map.of("query", invocation.args()[0]));
            return 0;
        }
        invocation.replyKey("commands.playerstate.realname.header", Map.of("count", matches.size()));
        matches.forEach(target -> invocation.replyKey("commands.playerstate.realname.entry", Map.of(
                "displayName", displayNames.displayName(target),
                "username", target.name()
        )));
        return matches.size();
    }

    static String normalize(String value) {
        var plain = MINI_TAG.matcher(LEGACY.matcher(value).replaceAll("")).replaceAll("");
        return Normalizer.normalize(plain, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);
    }

}

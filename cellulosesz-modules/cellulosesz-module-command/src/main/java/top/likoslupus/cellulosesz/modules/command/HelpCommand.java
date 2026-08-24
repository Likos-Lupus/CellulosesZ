package top.likoslupus.cellulosesz.modules.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.argument.HelpSelectors;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.core.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.core.command.catalog.CommandCatalogEntry;
import top.likoslupus.cellulosesz.core.command.service.CommandAliasRegistry;
import top.likoslupus.cellulosesz.core.config.ConfigRegistry;

import java.util.*;

public final class HelpCommand implements CommandContributor {

    private static final String MODULE = "command";

    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            MODULE,
            "help",
            "cellulosesz.command.help",
            CommandSourceKind.ANY
    );

    private int executeSelector(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            int page,
            boolean explicitQueryPage
    ) throws CommandSyntaxException {
        var selector = HelpSelectors.parse(
                StringArgumentType.getString(command, "queryOrPage")
        );

        if (explicitQueryPage) {
            return execute(
                    registration,
                    command,
                    HelpSelectors.requireQuery(selector),
                    page
            );
        }

        return switch (selector) {
            case HelpSelectors.Selection.Page(var value) -> execute(
                    registration,
                    command,
                    "",
                    value
            );
            case HelpSelectors.Selection.Query(var value) -> execute(
                    registration,
                    command,
                    value,
                    1
            );
        };
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            String query,
            int page
    ) {
        return CommandExecutions.sync(
                registration,
                command,
                DESCRIPTOR,
                "help",
                policy -> show(
                        registration,
                        policy,
                        query,
                        page
                )
        );
    }

    private int show(
            CommandRegistrationContext registration,
            MinecraftCommandPolicyContext policy,
            String rawQuery,
            int page
    ) {
        var query = rawQuery.toLowerCase(Locale.ROOT);

        var renderer = registration.services()
                .require(MessageRenderer.class);

        var locale = locale(registration, policy);

        var aliases = registration.services()
                .require(CommandAliasRegistry.class);

        var visible = registration.services()
                .require(CommandCatalog.class)
                .commands()
                .stream()
                .filter(entry -> visible(policy, entry))
                .map(entry -> view(
                        entry,
                        aliases,
                        renderer,
                        locale
                ))
                .filter(entry ->
                        query.isBlank()
                                || entry.name().contains(query)
                                || entry.aliases()
                                .stream()
                                .anyMatch(alias ->
                                        alias.contains(query)
                                )
                                || entry.description()
                                .toLowerCase(Locale.ROOT)
                                .contains(query)
                                || entry.usage()
                                .toLowerCase(Locale.ROOT)
                                .contains(query)
                )
                .sorted(Comparator.comparing(HelpEntry::name))
                .toList();

        if (visible.isEmpty()) {
            policy.error(
                    LocalizedMessage.of(
                            "commands.command.help-empty",
                            MessageArguments.builder().add(rawQuery).build()
                    )
            );

            return 0;
        }

        var pageSize = registration.services()
                .require(ConfigRegistry.class)
                .require(
                        "module.command",
                        CommandConfig.class
                )
                .helpPageSize;

        var pages = Math.toIntExact(
                ((long) visible.size() + pageSize - 1L) / pageSize
        );

        if (page > pages) {
            policy.error(
                    LocalizedMessage.of(
                            "commands.common.page-out-of-range",
                            MessageArguments.builder().add(pages).build()
                    )
            );

            return 0;
        }

        var startLong = (long) (page - 1) * pageSize;

        if (startLong >= visible.size()) {
            policy.error(
                    LocalizedMessage.of(
                            "commands.common.page-out-of-range",
                            MessageArguments.builder().add(pages).build()
                    )
            );

            return 0;
        }

        var start = Math.toIntExact(startLong);
        var end = (int) Math.min(
                startLong + pageSize,
                visible.size()
        );

        policy.reply(
                LocalizedMessage.of(
                        "commands.command.help-header",
                        MessageArguments.builder()
                                .add(page)
                                .add(pages)
                                .add(rawQuery)
                                .build()
                )
        );

        visible.subList(start, end)
                .forEach(entry -> policy.reply(
                        LocalizedMessage.of(
                                "commands.command.help-entry-detail",
                                MessageArguments.builder()
                                        .add(entry.name())
                                        .add(entry.description())
                                        .add(entry.usage())
                                        .add(String.join(", ", entry.aliases()))
                                        .build()
                        )
                ));

        return 1;
    }

    private boolean visible(
            MinecraftCommandPolicyContext policy,
            CommandCatalogEntry entry
    ) {
        var descriptor = entry.descriptor();

        if (!descriptor.permission().isBlank()
                && !policy.hasPermission(descriptor.permission())) {
            return false;
        }

        return switch (descriptor.requiredSourceKind()) {
            case ANY -> true;
            case PLAYER_ONLY -> policy.player();
            case CONSOLE_ONLY -> !policy.player();
        };
    }

    private HelpEntry view(
            CommandCatalogEntry entry,
            CommandAliasRegistry aliases,
            MessageRenderer renderer,
            String locale
    ) {
        var names = new LinkedHashSet<String>();

        entry.aliases().forEach(alias ->
                names.add(alias.toLowerCase(Locale.ROOT))
        );

        aliases.aliases(entry.descriptor().canonicalName())
                .forEach(alias ->
                        names.add(alias.toLowerCase(Locale.ROOT))
                );

        return new HelpEntry(
                entry.descriptor()
                        .canonicalName()
                        .toLowerCase(Locale.ROOT),
                new ArrayList<>(names),
                localized(
                        renderer,
                        locale,
                        entry.description()
                ),
                localized(
                        renderer,
                        locale,
                        entry.usage()
                )
        );
    }

    private String localized(
            MessageRenderer renderer,
            String locale,
            String value
    ) {
        return value.indexOf(' ') < 0 && value.contains(".")
                ? renderer.render(locale, value).plainText()
                : value;
    }

    private String locale(
            CommandRegistrationContext registration,
            MinecraftCommandPolicyContext policy
    ) {
        var resolver = registration.services()
                .require(LocaleResolver.class);

        var uuid = policy.playerUuid();
        var online = uuid == null
                ? null
                : registration.services()
                        .require(PlayerDirectory.class)
                        .onlinePlayer(uuid);

        return online != null
                ? resolver.locale(online)
                : resolver.consoleLocale();
    }

    @Override
    public String moduleId() {
        return MODULE;
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var root = Commands.literal("help")
                .executes(command -> execute(
                        context,
                        command,
                        "",
                        1
                ))
                .then(Commands.argument(
                                        "queryOrPage",
                                        StringArgumentType.word()
                                )
                                .executes(command -> executeSelector(
                                        context,
                                        command,
                                        1,
                                        false
                                ))
                                .then(Commands.argument(
                                                        "queryPage",
                                                        IntegerArgumentType.integer(1)
                                                )
                                                .executes(command -> executeSelector(
                                                        context,
                                                        command,
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "queryPage"
                                                        ),
                                                        true
                                                ))
                                )
                );

        context.registerDirect(
                moduleId(),
                DESCRIPTOR,
                List.of(),
                "commands.description.help",
                "/help [page|query] [page]",
                root
        );
    }

    private record HelpEntry(
            String name,
            List<String> aliases,
            String description,
            String usage
    ) {

    }

}

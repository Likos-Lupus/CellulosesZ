package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.admin.application.JailCommandService;
import top.likoslupus.cellulosesz.modules.admin.domain.Jail;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class DelJailCommand implements CommandContributor {

    private final JailCommandService service;

    public DelJailCommand(JailCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "deljail",
                "cellulosesz.admin.jail.delete",
                CommandSourceKind.ANY
        );

        var argument = Commands.argument(
                        "name",
                        StringArgumentType.word()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                () -> service.jails()
                                        .stream()
                                        .map(Jail::name)
                                        .toList(),
                                builder
                        )
                )
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "deljail",
                        _ -> service.delete(
                                StringArgumentType.getString(
                                        command,
                                        "name"
                                )
                        )
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.deljail",
                "/deljail <name>",
                Commands.literal("deljail").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}

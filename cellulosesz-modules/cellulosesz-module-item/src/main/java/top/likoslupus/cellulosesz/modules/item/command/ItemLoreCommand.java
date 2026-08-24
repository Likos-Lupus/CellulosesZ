package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.validation.Checks;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.ItemRuntimeSettings;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ItemLoreCommand implements CommandContributor {

    private static final int MAXIMUM_LINE_LENGTH = 256;
    private static final int MAXIMUM_TOTAL_LENGTH = 2048;

    private final ItemCommandService service;
    private final ItemRuntimeSettings config;

    public ItemLoreCommand(
            ItemCommandService service,
            ItemRuntimeSettings config
    ) {
        this.service = requireNonNull(service, "service");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "itemlore",
                "cellulosesz.item.lore",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("itemlore")
                .then(Commands.literal("clear")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                List.of()
                        ))
                )
                .then(Commands.argument(
                                        "text",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        parse(StringArgumentType.getString(
                                                command,
                                                "text"
                                        ))
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.itemlore",
                "/itemlore <clear|text...>",
                root
        );
    }

    private List<RichText> parse(String text) {
        text = Checks.requireMaxLength(
                text,
                MAXIMUM_TOTAL_LENGTH,
                "lore"
        );

        Checks.requireNoControlCharacters(
                text.replace("\\n", ""),
                "lore"
        );

        var lines = Arrays.stream(text.split("\\\\n", -1))
                .limit(config.maxLoreLines() + 1L)
                .toList();

        if (lines.size() > config.maxLoreLines()) {
            throw new IllegalArgumentException(
                    "lore line count exceeds configured maximum"
            );
        }

        return lines.stream()
                .map(line -> RichText.plain(
                        Checks.requireMaxLength(
                                line,
                                MAXIMUM_LINE_LENGTH,
                                "lore line"
                        )
                ))
                .toList();
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            List<RichText> lore
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "itemlore lines=" + lore.size(),
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value -> service.setLore(value, lore))
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "player-only"
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

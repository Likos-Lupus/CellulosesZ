package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.item.FireworkItemRequest;
import top.likoslupus.cellulosesz.common.item.FireworkShape;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class FireworkCommand implements CommandContributor {

    private final ItemCommandService service;

    public FireworkCommand(ItemCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "firework",
                "cellulosesz.item.firework",
                CommandSourceKind.PLAYER_ONLY
        );

        var flicker = Commands.argument(
                        "flicker",
                        BoolArgumentType.bool()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        request(
                                command,
                                StringArgumentType.getString(
                                        command,
                                        "fadeColors"
                                ),
                                BoolArgumentType.getBool(
                                        command,
                                        "trail"
                                ),
                                BoolArgumentType.getBool(
                                        command,
                                        "flicker"
                                )
                        )
                ));

        var trail = Commands.argument(
                        "trail",
                        BoolArgumentType.bool()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        request(
                                command,
                                StringArgumentType.getString(
                                        command,
                                        "fadeColors"
                                ),
                                BoolArgumentType.getBool(
                                        command,
                                        "trail"
                                ),
                                false
                        )
                ))
                .then(flicker);

        var fadeColors = Commands.argument(
                        "fadeColors",
                        StringArgumentType.word()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        request(
                                command,
                                StringArgumentType.getString(
                                        command,
                                        "fadeColors"
                                ),
                                false,
                                false
                        )
                ))
                .then(trail);

        var colors = Commands.argument(
                        "colors",
                        StringArgumentType.word()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        request(command, "", false, false)
                ))
                .then(fadeColors);

        var effect = Commands.literal("effect")
                .then(Commands.argument(
                                        "shape",
                                        StringArgumentType.word()
                                )
                                .then(colors)
                );

        var root = Commands.literal("firework")
                .then(Commands.literal("clear")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                FireworkItemRequest.clear()
                        ))
                )
                .then(Commands.literal("power")
                        .then(Commands.argument(
                                                "power",
                                                IntegerArgumentType.integer(1, 3)
                                        )
                                        .executes(command -> execute(
                                                context,
                                                command,
                                                descriptor,
                                                FireworkItemRequest.power(
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "power"
                                                        )
                                                )
                                        ))
                        )
                )
                .then(effect);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.firework",
                "/firework <clear|power <1..3>|effect <shape> "
                        + "<colors> [fadeColors] [trail] [flicker]>",
                root
        );
    }

    private static FireworkItemRequest request(
            CommandContext<?> command,
            String fades,
            boolean trail,
            boolean flicker
    ) {
        return FireworkItemRequest.effect(
                FireworkShape.valueOf(
                        StringArgumentType.getString(command, "shape")
                                .toUpperCase(Locale.ROOT)
                ),
                colors(StringArgumentType.getString(command, "colors")),
                fades.isBlank()
                        ? List.of()
                        : colors(fades),
                trail,
                flicker
        );
    }

    private static List<Integer> colors(String input) {
        var result = new ArrayList<Integer>();

        for (var token : input.split(",")) {
            var value = token.trim();

            if (value.startsWith("#")) {
                value = value.substring(1);
            }

            var color = Integer.parseInt(value, 16);

            if (color < 0 || color > 0xFFFFFF) {
                throw new IllegalArgumentException(
                        "color is outside RGB range"
                );
            }

            result.add(color);
        }

        return List.copyOf(result);
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            FireworkItemRequest request
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "firework",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player.<PlatformResult<?>>map(value ->
                                    service.firework(value, request)
                            )
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

package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.BookAction;
import top.likoslupus.cellulosesz.api.item.BookRequest;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.validation.TextChecks;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BookCommand implements CommandContributor {

    private static final int MAXIMUM_TITLE = 64;
    private static final int MAXIMUM_AUTHOR = 64;

    private final InventoryCommandService service;
    private final InventoryPlatformService inventory;

    public BookCommand(
            InventoryCommandService service,
            InventoryPlatformService inventory
    ) {
        this.service = requireNonNull(service, "service");
        this.inventory = requireNonNull(inventory, "inventory");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "book",
                "cellulosesz.command.book",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("book")
                .executes(command -> toggle(context, command, descriptor))
                .then(Commands.literal("title")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.book.title"
                        ))
                        .then(Commands.argument(
                                                "title",
                                                StringArgumentType.greedyString()
                                        )
                                        .executes(command -> mutate(
                                                context,
                                                command,
                                                descriptor,
                                                BookAction.SET_TITLE,
                                                TextChecks.requireMaxLength(
                                                        TextChecks.requireNoControlCharacters(
                                                                StringArgumentType.getString(
                                                                        command,
                                                                        "title"
                                                                ),
                                                                "title"
                                                        ),
                                                        MAXIMUM_TITLE,
                                                        "title"
                                                )
                                        ))
                        )
                )
                .then(Commands.literal("author")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.book.author"
                        ))
                        .then(Commands.argument(
                                                "author",
                                                StringArgumentType.greedyString()
                                        )
                                        .executes(command -> mutate(
                                                context,
                                                command,
                                                descriptor,
                                                BookAction.SET_AUTHOR,
                                                TextChecks.requireMaxLength(
                                                        TextChecks.requireNoControlCharacters(
                                                                StringArgumentType.getString(
                                                                        command,
                                                                        "author"
                                                                ),
                                                                "author"
                                                        ),
                                                        MAXIMUM_AUTHOR,
                                                        "author"
                                                )
                                        ))
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.book",
                "/book | /book title <title...> | /book author <author...>",
                root
        );
    }

    private int toggle(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "book toggle",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var currentPlayer = player.orElseThrow();
                    var details = inventory.heldBook(currentPlayer);

                    if (!details.successful() || details.value().isEmpty()) {
                        return details;
                    }

                    var book = details.value().orElseThrow();

                    if (book.written()
                            && book.author().isPresent()
                            && !book.author().orElseThrow()
                            .equalsIgnoreCase(currentPlayer.name())
                            && !policy.hasPermission(
                            "cellulosesz.command.book.others"
                    )) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.PERMISSION_DENIED,
                                "book-owner"
                        );
                    }

                    return service.book(
                            currentPlayer,
                            new BookRequest(
                                    book.written()
                                            ? BookAction.UNLOCK
                                            : BookAction.SIGN,
                                    "",
                                    currentPlayer.name()
                            )
                    );
                }
        );
    }

    private int mutate(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            BookAction action,
            String value
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "book " + action.name().toLowerCase(),
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player.<PlatformResult<?>>map(valuePlayer ->
                                    service.book(
                                            valuePlayer,
                                            new BookRequest(
                                                    action,
                                                    value,
                                                    valuePlayer.name()
                                            )
                                    )
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

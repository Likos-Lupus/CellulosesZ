package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.BookAction;
import top.likoslupus.cellulosesz.api.item.BookRequest;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;

public final class BookCommand implements CellCommand {

    private final PlatformService platform;
    private final InventoryPlatformService inventory;

    public BookCommand(
            PlatformService platform,
            InventoryPlatformService inventory
    ) {
        this.platform = platform;
        this.inventory = inventory;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.book";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/book | /book title <title> | /book author <author>";
    }

    @Override
    public String name() {
        return "book";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length == 1 || invocation.args().length > 2) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        var before = inventory.heldBook(player);
        if (!before.successful() || before.value().isEmpty()) {
            invocation.errorKey("commands.item.book.not-book");
            return 0;
        }
        final BookRequest request;
        var details = before.value().orElseThrow();
        if (details.written()
                && details.author().isPresent()
                && !details.author().orElseThrow().equalsIgnoreCase(player.name())
                && !invocation.hasPermission("cellulosesz.command.book.others")) {
            return denied(invocation);
        }
        if (invocation.args().length == 0) {
            request = new BookRequest(details.written() ? BookAction.UNLOCK : BookAction.SIGN, "", player.name());
        } else if (invocation.args()[0].equalsIgnoreCase("title")) {
            if (!invocation.hasPermission("cellulosesz.command.book.title")) return denied(invocation);
            request = new BookRequest(BookAction.SET_TITLE, invocation.args()[1], player.name());
        } else if (invocation.args()[0].equalsIgnoreCase("author")) {
            if (!invocation.hasPermission("cellulosesz.command.book.author")) return denied(invocation);
            request = new BookRequest(BookAction.SET_AUTHOR, invocation.args()[1], player.name());
        } else return usage(invocation);
        var result = inventory.mutateBook(player, request);
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.item.book.success", Map.of("action", request.action()
                .name()
                .toLowerCase(java.util.Locale.ROOT)));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.book.usage", Map.of("usage", usage()));
        return 0;
    }

    private int denied(CommandInvocation invocation) {
        invocation.errorKey("commands.common.no-permission");
        return 0;
    }

}

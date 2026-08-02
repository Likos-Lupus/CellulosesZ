package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.TextService;

import java.util.Locale;
import java.util.Optional;

final class SignHandlerSupport {

    private SignHandlerSupport() {
    }

    static SignUseResult noArguments(SignUseContext context, String formatKey) {
        for (var index = 1; index < context.lines().size(); index++) {
            if (!context.line(index).isBlank()) {
                return SignUseResult.failure(formatKey);
            }
        }
        return SignUseResult.success("service.sign.valid");
    }

    static SignUseResult validateItem(
            ItemService items,
            PlatformResult<ItemDescriptor> parsed,
            String formatKey
    ) {
        if (!parsed.successful() || parsed.value().isEmpty()) {
            return itemFailure(parsed.status(), formatKey);
        }
        var item = parsed.value().orElseThrow();
        var valid = items.valid(item);
        if (!valid.successful() || valid.value().isEmpty()) {
            return itemFailure(valid.status(), formatKey);
        }
        if (!valid.value().orElseThrow()) {
            return SignUseResult.failure(formatKey);
        }
        if (items.blacklisted(item)) {
            return SignUseResult.failure(
                    "service.sign.item-blacklisted",
                    MessageArguments.of("item", item.normalizedItem())
            );
        }
        return SignUseResult.success("service.sign.valid");
    }

    static SignUseResult itemFailure(PlatformOperationStatus status, String formatKey) {
        return switch (status) {
            case INVALID_ARGUMENT, INVALID_INPUT, NOT_FOUND -> SignUseResult.failure(formatKey);
            default -> SignUseResult.failure("service.sign.execution-failed");
        };
    }

    static Optional<Long> parseTime(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "day" -> Optional.of(1000L);
            case "noon" -> Optional.of(6000L);
            case "night" -> Optional.of(13000L);
            case "midnight" -> Optional.of(18000L);
            default -> {
                try {
                    var value = Long.parseLong(input);
                    yield value >= 0L
                            ? Optional.of(value)
                            : Optional.empty();
                } catch (NumberFormatException exception) {
                    yield Optional.empty();
                }
            }
        };
    }

    static Optional<String> textPage(TextService texts, SignUseContext context) {
        var section = context.line(1).isBlank()
                ? "info"
                : context.line(1).toLowerCase(Locale.ROOT);
        var lines = switch (section) {
            case "info" -> texts.info();
            case "motd" -> texts.motd();
            case "rules" -> texts.rules();
            default -> texts.custom(section);
        };
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        var page = count(context.line(2), 1, Integer.MAX_VALUE).orElse(1);
        var pageSize = Math.max(1, texts.pageSize());
        final int from;
        try {
            from = Math.multiplyExact(page - 1, pageSize);
        } catch (ArithmeticException exception) {
            return Optional.empty();
        }
        if (from >= lines.size()) {
            return Optional.empty();
        }
        return Optional.of(String.join(
                "\n",
                lines.subList(from, Math.min(lines.size(), from + pageSize))
        ));
    }

    static Optional<Integer> count(String input, int minimum, int maximum) {
        if (input.isBlank()) {
            return Optional.empty();
        }
        try {
            var value = Integer.parseInt(input);
            return value >= minimum && value <= maximum
                    ? Optional.of(value)
                    : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    static SignUseResult admin(AdminResult result) {
        return result.success()
                ? SignUseResult.success(result.message())
                : SignUseResult.failure(result.message());
    }

    static MessageArguments itemPlaceholders(ItemDescriptor item) {
        return MessageArguments.builder()
                .put("count", item.count())
                .put("item", item.normalizedItem())
                .build();
    }

}

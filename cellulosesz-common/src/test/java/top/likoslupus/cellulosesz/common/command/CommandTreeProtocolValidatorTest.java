package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandTreeProtocolValidatorTest {

    private final CommandTreeProtocolValidator validator = new CommandTreeProtocolValidator();

    @Test
    void validate_standardArgument_completes() {
        initializeArgumentTypes();
        var root = LiteralArgumentBuilder.<CommandSourceStack>literal("safe")
                .then(RequiredArgumentBuilder
                        .argument(
                                "value",
                                StringArgumentType.word()
                        ))
                .build();

        assertDoesNotThrow(() -> validator.validate(List.of(
                new CommandRootLeaseManager.Lease(
                        "safe",
                        "safe",
                        "test",
                        CommandRootLeaseManager.LabelKind.CANONICAL,
                        root,
                        1L
                )
        )));
    }

    private static void initializeArgumentTypes() {
        BuiltInRegistries.COMMAND_ARGUMENT_TYPE.size();
    }

    @Test
    void validate_unknownArgument_reportsPathAndType() {
        initializeArgumentTypes();
        var root = LiteralArgumentBuilder.<CommandSourceStack>literal("unsafe")
                .then(RequiredArgumentBuilder
                        .argument(
                                "value",
                                new UnknownArgument()
                        ))
                .build();

        var failure = assertThrows(
                IllegalStateException.class, () -> validator.validate(
                        List.of(new CommandRootLeaseManager.Lease(
                                "unsafe",
                                "unsafe",
                                "test-owner",
                                CommandRootLeaseManager.LabelKind.CANONICAL,
                                root,
                                1L
                        ))
                )
        );

        assertTrue(failure.getMessage().contains("/unsafe <value>"));
        assertTrue(failure.getMessage().contains(UnknownArgument.class.getName()));
        assertTrue(failure.getMessage().contains("test-owner"));
    }

    private static final class UnknownArgument implements ArgumentType<String> {

        @Override
        public String parse(StringReader reader) {
            return reader.readUnquotedString();
        }

    }

}

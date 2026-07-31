package top.likoslupus.cellulosesz.fabric.architecture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registers all 42 Segment 3 production contributors in a real Brigadier dispatcher.
 */
final class Segment3CommandTreeParsingTest {

    private static final List<String> COMMAND_CLASSES = List.of(
            "top.likoslupus.cellulosesz.modules.admin.command.BanCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.BanIpCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.BurnCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.DelJailCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.ExtCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.IceCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.JailCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.JailedPlayersCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.JailsCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.KickCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.KickAllCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.KillCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.MuteCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.SetJailCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.SudoCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.SuicideCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.TempBanCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.TempBanIpCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.UnbanCommand",
            "top.likoslupus.cellulosesz.modules.admin.command.UnbanIpCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.BackCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.BottomCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.JumpCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.SetTprCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TopCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpaCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpaAllCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpaCancelCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpAcceptCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpaHereCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpAllCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpAutoCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpDenyCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpHereCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpoCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpOfflineCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpoHereCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpPosCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TprCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.TpToggleCommand",
            "top.likoslupus.cellulosesz.modules.teleport.command.WorldCommand"
    );

    private CommandDispatcher<CommandSourceStack> dispatcher;

    @BeforeEach
    void registerProductionTrees() throws ReflectiveOperationException {
        dispatcher = new CommandDispatcher<>();
        var context = new TreeContext(dispatcher, true);
        for (var className : COMMAND_CLASSES) command(className).register(context);
        assertEquals(42, COMMAND_CLASSES.size());
    }

    private static CommandContributor command(String className) throws ReflectiveOperationException {
        var type = Class.forName(className);
        var constructors = type.getDeclaredConstructors();
        if (constructors.length != 1) throw new IllegalStateException("Expected one constructor: " + className);
        var constructor = constructors[0];
        constructor.setAccessible(true);
        var parameters = constructor.getParameterTypes();
        var arguments = new Object[parameters.length];
        for (var index = 0; index < parameters.length; index++) arguments[index] = fixture(parameters[index]);
        return (CommandContributor) constructor.newInstance(arguments);
    }

    private static Object fixture(Class<?> type) {
        if (type == int.class) return 512;
        if (type == Duration.class) return Duration.ofDays(3650);
        if (type.isInterface()) return proxy(type);
        throw new IllegalArgumentException("Unsupported command constructor dependency: " + type.getName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (_, method, _) -> {
            var returnType = method.getReturnType();
            if (returnType == boolean.class) return false;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            if (returnType == double.class) return 0D;
            if (returnType == Optional.class) return Optional.empty();
            if (returnType == List.class) return List.of("Alice", "Bob");
            if (returnType == Set.class) return Set.of();
            if (returnType == CompletableFuture.class) return new CompletableFuture<>();
            return null;
        });
    }

    @Test
    void adminRoutesParseAndRejectMalformedShapes() {
        valid(
                "ban Alice", "ban Alice reason text", "banip 127.0.0.1", "banip Alice reason",
                "tempban Alice 1h", "tempbanip ::1 30m reason", "unban Alice", "unbanip ::1",
                "kick Alice", "kickall", "kickall reason", "mute Alice", "mute Alice off",
                "mute Alice 1h reason", "mute Alice reason text", "setjail jail1", "deljail jail1",
                "jails", "jails 2", "jailedplayers", "jailedplayers 2", "jail Alice off",
                "jail Alice jail1", "jail Alice jail1 1h reason", "togglejail Alice jail1",
                "burn Alice 10", "ext", "ext Alice", "ice", "ice Alice", "kill Alice",
                "suicide", "sudo Alice say hello"
        );
        invalid(
                "ban", "banip example.com", "tempban Alice off",
                "tempban Alice 0", "tempbanip Alice forever", "unban", "unbanip Alice",
                "kick", "mute", "mute Alice 0", "setjail", "jails 0", "jailedplayers 0",
                "jail Alice", "jail Alice jail1 0", "burn Alice -1", "burn Alice 999999999999",
                "kill", "sudo Alice"
        );
    }

    private void valid(String... commands) {
        Arrays.stream(commands).forEach(command ->
                assertDoesNotThrow(() -> dispatcher.execute(command, null), command));
    }

    private void invalid(String... commands) {
        Arrays.stream(commands).forEach(command ->
                assertThrows(CommandSyntaxException.class, () -> dispatcher.execute(command, null), command));
    }

    @Test
    void teleportRoutesParseAndDisambiguateLiterals() {
        valid(
                "back", "bottom", "jump", "top", "tp Alice", "tp Alice Bob", "tpa Alice",
                "tpahere Alice", "tpaall", "tpaccept", "tpaccept 00000000-0000-0000-0000-000000000001",
                "tpaccept Alice", "tpdeny Alice", "tpacancel Alice", "tpall", "tpall Alice",
                "tpauto", "tpauto off", "tptoggle", "tptoggle on", "tptoggle Alice",
                "tptoggle Alice off", "tphere Alice", "tpo Alice", "tpo Alice Bob",
                "tpoffline Alice", "tpohere Alice", "tppos 1 64 2",
                "tppos 1 64 2 minecraft:the_nether", "tpr", "world minecraft:overworld",
                "settpr minecraft:overworld center", "settpr minecraft:overworld center 10 20",
                "settpr minecraft:overworld minrange", "settpr minecraft:overworld minrange 100",
                "settpr minecraft:overworld maxrange 500"
        );
        invalid(
                "back extra", "tp", "tp Alice Bob Carol", "tpa", "tpahere", "tpaall extra",
                "tpaccept Alice extra", "tpdeny Alice extra", "tpacancel Alice extra", "tpauto maybe",
                "tptoggle Alice maybe", "tphere", "tpo", "tpoffline", "tpohere",
                "tppos NaN 64 2", "tppos 1 64", "tpr extra", "world",
                "settpr minecraft:overworld center 10", "settpr minecraft:overworld minrange -1",
                "settpr minecraft:overworld unknown"
        );
    }

    @Test
    void rootPermissionHidesACommand() throws ReflectiveOperationException {
        var denied = new CommandDispatcher<CommandSourceStack>();
        command("top.likoslupus.cellulosesz.modules.admin.command.BanCommand")
                .register(new TreeContext(denied, false));
        assertThrows(CommandSyntaxException.class, () -> denied.execute("ban Alice", null));
    }

    @NullMarked
    private record TreeContext(
            CommandDispatcher<CommandSourceStack> dispatcher,
            boolean allowPermissions
    ) implements CommandRegistrationContext {

        @Override
        public ServiceRegistry services() {
            return proxy(ServiceRegistry.class);
        }

        @Override
        public PermissionService permissions() {
            return new PermissionService() {
                @Override
                public boolean has(Object source, String permission) {
                    return allowPermissions;
                }

                @Override
                public int intOption(Object source, String key, int fallback) {
                    return fallback;
                }

                @Override
                public boolean boolOption(Object source, String key, boolean fallback) {
                    return fallback;
                }

                @Override
                public Optional<String> stringOption(Object source, String key) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public boolean moduleEnabled(String moduleId) {
            return true;
        }

        @Override
        public Optional<CellPlayer> player(CommandSourceStack source) {
            return Optional.empty();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return List.of("Alice", "Bob");
        }

        @Override
        public CommandNode<CommandSourceStack> registerDirect(
                String owner, CommandDescriptor descriptor,
                List<String> semanticRoots, String description, String usage,
                LiteralArgumentBuilder<CommandSourceStack> root
        ) {
            root.requires(source -> descriptor.permission().isBlank()
                    || permissions().has(source, descriptor.permission()));
            return dispatcher.register(root);
        }

        @Override
        public CommandNode<CommandSourceStack> registerSemantic(
                String owner, CommandDescriptor descriptor,
                String label, LiteralArgumentBuilder<CommandSourceStack> root
        ) {
            return dispatcher.register(root);
        }

        @Override
        public void registerAlias(
                String owner, CommandDescriptor descriptor, String label,
                CommandNode<CommandSourceStack> target
        ) {
            dispatcher.register(Commands.literal(label).redirect(target));
        }

        @Override
        public int execute(
                CommandContext<CommandSourceStack> command, CommandDescriptor descriptor,
                String auditSummary, Function<MinecraftCommandPolicyContext, Integer> terminal
        ) {
            return 1;
        }

        @Override
        public void internalFailure(MinecraftCommandPolicyContext policy, Throwable failure) {
            throw new AssertionError(failure);
        }

    }

}

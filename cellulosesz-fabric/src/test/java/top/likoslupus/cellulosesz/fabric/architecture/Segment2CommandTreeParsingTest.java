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
import sun.misc.Unsafe;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandSettings;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandSettings;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registers the production Segment 2 contributors into a real Brigadier dispatcher.
 */
final class Segment2CommandTreeParsingTest {

    private static final Unsafe UNSAFE = unsafe();
    private static final EconomyCommandSettings ECONOMY_SETTINGS = new EconomyCommandSettings(
            2,
            BigDecimal.ZERO,
            new BigDecimal("1000000.00"),
            new BigDecimal("0.01"),
            new BigDecimal("100.00"),
            true,
            false,
            10,
            10,
            1L
    );
    private static final PlayerStateCommandSettings PLAYER_STATE_SETTINGS = new PlayerStateCommandSettings(
            200,
            1_000,
            20,
            0.1D,
            10.0D,
            512,
            20,
            300_000L,
            0L,
            20L,
            true,
            true,
            true,
            true
    );

    private static final List<String> COMMAND_CLASSES = List.of(
            "top.likoslupus.cellulosesz.modules.command.CellulosesZCommand",
            "top.likoslupus.cellulosesz.modules.command.HelpCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.BroadcastCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.BroadcastWorldCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.HelpOpCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.IgnoreCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.ListCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.MailCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.MeCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.MsgCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.MsgToggleCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.ReplyCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.ReplyToggleCommand",
            "top.likoslupus.cellulosesz.modules.messaging.command.SocialSpyCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.BalanceCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.BalanceTopCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.EcoCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.PayCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.PayConfirmToggleCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.PayToggleCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.SellCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.SetWorthCommand",
            "top.likoslupus.cellulosesz.modules.economy.command.WorthCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.AfkCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.CompassCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.DepthCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.ExpCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.FeedCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.FlyCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.GameModeCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.GetPosCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.GodCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.HealCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.NearCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.NickCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.PingCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.PlaytimeCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.PTimeCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.PWeatherCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.RealNameCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.RestCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.SeenCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.SpeedCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.VanishCommand",
            "top.likoslupus.cellulosesz.modules.playerstate.command.WhoisCommand"
    );

    private CommandDispatcher<CommandSourceStack> dispatcher;

    private static Unsafe unsafe() {
        try {
            var field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @BeforeEach
    void registerProductionTrees() throws ReflectiveOperationException {
        dispatcher = new CommandDispatcher<>();
        var context = new TreeContext(dispatcher, true);
        for (var className : COMMAND_CLASSES) {
            command(className).register(context);
        }
        assertEquals(45, COMMAND_CLASSES.size());
    }

    private static CommandContributor command(String className) throws ReflectiveOperationException {
        var type = Class.forName(className);
        var constructors = type.getDeclaredConstructors();
        if (constructors.length != 1) throw new IllegalStateException("Expected one constructor: " + className);
        var constructor = constructors[0];
        constructor.setAccessible(true);
        var parameters = constructor.getParameterTypes();
        var arguments = new Object[parameters.length];
        for (var index = 0; index < parameters.length; index++) {
            arguments[index] = fixture(type, parameters[index]);
        }
        return (CommandContributor) constructor.newInstance(arguments);
    }

    private static Object fixture(Class<?> commandType, Class<?> parameterType) throws InstantiationException {
        if (parameterType == int.class) return 512;
        if (parameterType == EconomyCommandSettings.class) return ECONOMY_SETTINGS;
        if (parameterType == PlayerStateCommandSettings.class) return PLAYER_STATE_SETTINGS;
        if (parameterType == Supplier.class) {
            if (commandType.getSimpleName().equals("MailCommand")) {
                return (Supplier<List<String>>) () -> List.of("Alice", "Bob");
            }
            return (Supplier<EconomyCommandSettings>) () -> ECONOMY_SETTINGS;
        }
        if (parameterType.isInterface()) return proxy(parameterType);
        return UNSAFE.allocateInstance(parameterType);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (_, method, _) -> {
                    var returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == double.class) return 0D;
                    if (returnType == Optional.class) return Optional.empty();
                    if (returnType == List.class) return List.of();
                    if (returnType == Set.class) return Set.of();
                    if (returnType == CompletableFuture.class) return new CompletableFuture<>();
                    return null;
                }
        );
    }

    @Test
    void everySegmentTwoCanonicalRootHasAValidProductionRoute() {
        valid(
                "cellulosesz", "help", "broadcast hello", "broadcastworld minecraft:overworld hello",
                "helpop help", "ignore Alice", "list", "mail", "me waves", "msg Alice hello",
                "msgtoggle", "r hello", "rtoggle", "socialspy", "balance", "balancetop",
                "eco give Alice 10.50", "pay Alice 10.50", "payconfirmtoggle", "paytoggle",
                "sell hand", "setworth minecraft:stone 1.25", "worth minecraft:stone", "afk",
                "compass", "depth", "exp show", "feed", "fly", "gamemode survival", "getpos",
                "god", "heal", "near", "nick Alice", "ping", "playtime", "ptime day",
                "pweather clear", "realname ali", "rest", "seen Alice", "speed 1.0", "vanish",
                "whois Alice"
        );
    }

    private void valid(String... commands) {
        Arrays.stream(commands).forEach(command -> assertDoesNotThrow(
                () -> dispatcher.execute(command, null),
                command
        ));
    }

    @Test
    void requiredAmbiguousShapesUseDistinctTypedBranches() {
        valid(
                "help 2", "help economy", "help economy 2",
                "rtoggle on", "rtoggle Alice", "rtoggle Alice off",
                "socialspy on", "socialspy Alice", "socialspy Alice off",
                "fly on", "fly Alice", "fly Alice off",
                "god on", "god Alice", "god Alice off",
                "vanish on", "vanish Alice", "vanish Alice off",
                "balancetop 2", "balancetop 2 10.50", "balancetop 2 10.50 100",
                "pay Alice 10", "pay Alice,Bob 10", "pay Alice 10 token",
                "sell hand 2", "sell all", "sell minecraft:stone", "sell minecraft:stone 10",
                "worth hand", "worth inventory", "worth minecraft:stone 10",
                "exp show Alice", "exp reset", "exp reset Alice", "exp set 100", "exp set Alice 30L",
                "exp give 100", "exp give Alice 30L", "exp take 100", "exp take Alice 30L",
                "ptime 6000", "ptime night"
        );
        invalid(
                "help 0", "help economy zero", "balancetop 0", "sell all 2",
                "exp set -1", "ptime -1"
        );
    }

    private void invalid(String... commands) {
        Arrays.stream(commands).forEach(command -> assertThrows(
                CommandSyntaxException.class,
                () -> dispatcher.execute(command, null),
                command
        ));
    }

    @Test
    void aliasesRedirectToCanonicalTrees() {
        valid(
                "cellz", "cz", "tell Alice hello", "w Alice hello", "reply hello",
                "bal", "money", "baltop", "pong", "v"
        );
    }

    @Test
    void rootPermissionsHideCommandsBeforeExecution() throws ReflectiveOperationException {
        var denied = new CommandDispatcher<CommandSourceStack>();
        var context = new TreeContext(denied, false);
        command("top.likoslupus.cellulosesz.modules.economy.command.BalanceCommand").register(context);
        assertThrows(CommandSyntaxException.class, () -> denied.execute("balance", null));
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
                String owner,
                CommandDescriptor descriptor,
                List<String> semanticRoots,
                String description,
                String usage,
                LiteralArgumentBuilder<CommandSourceStack> root
        ) {
            root.requires(source -> descriptor.permission().isBlank()
                    || permissions().has(source, descriptor.permission()));
            return dispatcher.register(root);
        }

        @Override
        public CommandNode<CommandSourceStack> registerSemantic(
                String owner,
                CommandDescriptor descriptor,
                String label,
                LiteralArgumentBuilder<CommandSourceStack> root
        ) {
            return dispatcher.register(root);
        }

        @Override
        public void registerAlias(
                String owner,
                CommandDescriptor descriptor,
                String label,
                CommandNode<CommandSourceStack> target
        ) {
            dispatcher.register(Commands.literal(label).redirect(target));
        }

        @Override
        public int execute(
                CommandContext<CommandSourceStack> command,
                CommandDescriptor descriptor,
                String auditSummary,
                Function<MinecraftCommandPolicyContext, Integer> terminal
        ) {
            return 1;
        }

        @Override
        public void internalFailure(MinecraftCommandPolicyContext policy, Throwable failure) {
            throw new AssertionError(failure);
        }

    }

}

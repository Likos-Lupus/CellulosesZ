package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNamePlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.user.UserConfig;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class DefaultDisplayNameService implements DisplayNameService {

    private static final Pattern LEGACY = Pattern.compile("(?i)[&§](?:#[0-9a-f]{6}|[0-9a-fk-or])");
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private final DisplayNamePlatformService platform;
    private final PlayerDirectory players;
    private final UserService users;
    private final PermissionService permissions;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private volatile Settings settings;

    public DefaultDisplayNameService(
            DisplayNamePlatformService platform,
            PlayerDirectory players,
            UserService users,
            PermissionService permissions,
            MessageRenderer renderer,
            LocaleResolver locales,
            UserConfig config
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.players = requireNonNull(players, "players");
        this.users = requireNonNull(users, "users");
        this.permissions = requireNonNull(permissions, "permissions");
        this.renderer = requireNonNull(renderer, "renderer");
        this.locales = requireNonNull(locales, "locales");
        this.settings = Settings.from(config);
    }

    public void validateConfiguration(UserConfig candidate) {
        Settings.from(candidate);
    }

    public void configure(UserConfig candidate) {
        settings = Settings.from(candidate);
    }

    private record Settings(
            String nicknamePrefix,
            int minLength,
            int maxLength,
            Pattern allowedPattern,
            Set<String> blacklist,
            String colorPermission
    ) {

        private static Settings from(UserConfig source) {
            var display = requireNonNull(
                    requireNonNull(source, "config").displayName,
                    "config.displayName"
            );
            if (display.minLength < 0 || display.maxLength < display.minLength) {
                throw new IllegalArgumentException("Invalid display-name length range");
            }

            var blacklist = requireNonNull(display.blacklist, "displayName.blacklist")
                    .stream()
                    .map(value -> requireNonNull(value, "blacklist entry")
                            .trim()
                            .toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isEmpty())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

            return new Settings(
                    requireNonNull(display.nicknamePrefix, "displayName.nicknamePrefix"),
                    display.minLength,
                    display.maxLength,
                    Pattern.compile(requireNonNull(
                            display.allowedPattern,
                            "displayName.allowedPattern"
                    )),
                    blacklist,
                    requireNonNull(display.colorPermission, "displayName.colorPermission")
            );
        }

    }

    @Override
    public RichText displayName(CellPlayer player) {
        return displayName(player.uuid(), player.name(), player);
    }

    @Override
    public RichText displayName(UUID uuid, String fallbackName) {
        var online = players.onlinePlayer(uuid);
        return online
                .map(player -> displayName(uuid, fallbackName, player))
                .orElseGet(() -> displayName(uuid, fallbackName, null));
    }

    @Override
    public String plainDisplayName(CellPlayer player) {
        return displayName(player).plainText();
    }

    @Override
    public boolean validNickname(CellPlayer player, String nickname) {
        var current = settings;
        var plain = stripFormatting(nickname).trim();
        var normalized = plain.toLowerCase(Locale.ROOT);
        return plain.length() >= current.minLength()
                && plain.length() <= current.maxLength()
                && !current.blacklist().contains(normalized)
                && current.allowedPattern().matcher(plain).matches();
    }

    @Override
    public String sanitizeNickname(CellPlayer player, String nickname) {
        var current = settings;
        var value = nickname.trim();

        if (!permissions.has(player, current.colorPermission())) {
            value = stripFormatting(value);
        }
        return value;
    }

    @Override
    public void refresh(CellPlayer player) {
        platform.setDisplayName(player, displayName(player));
        platform.refreshPlayerInfo(player);
    }

    @Override
    public void refreshAll() {
        players.onlinePlayers().forEach(this::refresh);
    }

    private RichText displayName(
            UUID uuid,
            String fallbackName,
            @Nullable CellPlayer online
    ) {
        var nickname = users.cached(uuid)
                .flatMap(user -> Optional.ofNullable(user.state().nickname()))
                .filter(value -> !value.isBlank());
        if (nickname.isEmpty()) {
            return RichText.plain(fallbackName);
        }

        var current = settings;
        var value = nickname.orElseThrow();
        var safe = online == null
                ? value
                : sanitizeNickname(online, value);
        var locale = online == null
                ? locales.consoleLocale()
                : locales.locale(online);

        return renderer.renderInline(locale, current.nicknamePrefix() + safe);
    }

    private String stripFormatting(String value) {
        return TAGS
                .matcher(LEGACY.matcher(value).replaceAll(""))
                .replaceAll("");
    }

}

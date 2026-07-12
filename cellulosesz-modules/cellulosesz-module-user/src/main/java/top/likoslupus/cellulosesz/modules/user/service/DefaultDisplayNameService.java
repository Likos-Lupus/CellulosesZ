package top.likoslupus.cellulosesz.modules.user.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.user.UserConfig;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class DefaultDisplayNameService implements DisplayNameService {

    private static final Pattern LEGACY = Pattern.compile("(?i)[&§](?:#[0-9a-f]{6}|[0-9a-fk-or])");
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private final PlatformService platform;
    private final UserService users;
    private final PermissionService permissions;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final UserConfig config;

    public DefaultDisplayNameService(
            PlatformService platform,
            UserService users,
            PermissionService permissions,
            MessageRenderer renderer,
            LocaleResolver locales,
            UserConfig config
    ) {
        this.platform = platform;
        this.users = users;
        this.permissions = permissions;
        this.renderer = renderer;
        this.locales = locales;
        this.config = config;
    }

    @Override
    public RichText displayName(CellPlayer player) {
        return displayName(player.uuid(), player.name(), player);
    }

    @Override
    public RichText displayName(UUID uuid, String fallbackName) {
        var online = platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(uuid))
                .findFirst();
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
        var plain = stripFormatting(nickname).trim();
        if (plain.length() < config.displayName.minLength
                || plain.length() > config.displayName.maxLength
                || config.displayName.blacklist.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(plain.toLowerCase(Locale.ROOT)))
        ) {
            return false;
        }

        try {
            return Pattern.compile(config.displayName.allowedPattern).matcher(plain).matches();
        } catch (PatternSyntaxException _) {
            return false;
        }
    }

    @Override
    public String sanitizeNickname(CellPlayer player, String nickname) {
        var value = nickname.trim();
        if (!permissions.has(player.nativeHandle(), config.displayName.colorPermission)) {
            value = stripFormatting(value);
        }
        return value;
    }

    @Override
    public void refresh(CellPlayer player) {
        platform.setDisplayName(player, displayName(player));
    }

    @Override
    public void refreshAll() {
        platform.onlinePlayers().forEach(this::refresh);
    }

    private RichText displayName(
            UUID uuid,
            String fallbackName,
            @Nullable CellPlayer online
    ) {
        var nickname = users.cached(uuid)
                .flatMap(user -> Optional.ofNullable(user.state.nickname))
                .filter(value -> !value.isBlank());
        if (nickname.isEmpty()) {
            return RichText.plain(fallbackName);
        }

        var value = nickname.orElseThrow();
        var safe = online == null
                ? value
                : sanitizeNickname(online, value);
        var locale = online == null
                ? locales.consoleLocale()
                : locales.locale(online);
        return renderer.renderInline(locale, config.displayName.nicknamePrefix + safe);
    }

    private String stripFormatting(String value) {
        return TAGS.matcher(LEGACY.matcher(value).replaceAll("")).replaceAll("");
    }

}

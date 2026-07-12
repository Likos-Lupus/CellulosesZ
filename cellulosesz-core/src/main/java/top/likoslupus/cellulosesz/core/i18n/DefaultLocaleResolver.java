package top.likoslupus.cellulosesz.core.i18n;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;

import java.util.Locale;

public final class DefaultLocaleResolver implements LocaleResolver {

    private final PlatformService platform;
    private String defaultLocale;
    private boolean useClientLocale;

    public DefaultLocaleResolver(
            PlatformService platform,
            String defaultLocale,
            boolean useClientLocale
    ) {
        this.platform = platform;
        configure(defaultLocale, useClientLocale);
    }

    public void configure(String defaultLocale, boolean useClientLocale) {
        this.defaultLocale = normalize(defaultLocale);
        this.useClientLocale = useClientLocale;
    }

    private String normalize(String locale) {
        if (locale.isBlank()) return "en_us";
        return locale
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
    }

    @Override
    public String locale(CommandInvocation invocation) {
        return platform.player(invocation)
                .map(this::locale)
                .orElse(defaultLocale);
    }

    @Override
    public String locale(CellPlayer player) {
        if (!useClientLocale) return defaultLocale;
        var locale = platform.locale(player);
        return locale.isBlank()
                ? defaultLocale
                : normalize(locale);
    }

    @Override
    public String consoleLocale() {
        return defaultLocale;
    }

}

package top.likoslupus.cellulosesz.core.i18n;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.text.ClientLocaleService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.validation.Checks;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class DefaultLocaleResolver implements LocaleResolver {

    private final ClientLocaleService clients;
    private volatile Settings settings;

    public DefaultLocaleResolver(
            ClientLocaleService clients,
            String defaultLocale,
            boolean useClientLocale
    ) {
        this.clients = requireNonNull(clients, "clients");
        configure(defaultLocale, useClientLocale);
    }

    public void configure(String defaultLocale, boolean useClientLocale) {
        settings = new Settings(normalize(defaultLocale), useClientLocale);
    }

    private String normalize(String locale) {
        var normalized = Checks.requireNonBlank(locale, "locale")
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
        return normalized.isBlank()
                ? "en_us"
                : normalized;
    }

    @Override
    public String locale(CellPlayer player) {
        var current = settings;
        if (!current.useClientLocale()) {
            return current.defaultLocale();
        }

        var locale = clients.clientLocale(player);
        return locale.isBlank()
                ? current.defaultLocale()
                : normalize(locale);
    }

    @Override
    public String consoleLocale() {
        return settings.defaultLocale();
    }

    private record Settings(
            String defaultLocale,
            boolean useClientLocale
    ) {

    }

}

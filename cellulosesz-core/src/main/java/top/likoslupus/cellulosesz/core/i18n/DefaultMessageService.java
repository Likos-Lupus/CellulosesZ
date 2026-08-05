package top.likoslupus.cellulosesz.core.i18n;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.MessageArgument;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class DefaultMessageService implements MessageRenderer {

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final String DEFAULT_LOCALE = "zh_cn";
    private static final String DEFAULT_FALLBACK = "en_us";
    private static final String DEFAULT_PRIMARY = "#55FF55";
    private static final String DEFAULT_SECONDARY = "#FFFF55";
    private static final String MISSING_KEY = "messages.missing";
    private static final String EMERGENCY_MESSAGE = "<red>A message could not be rendered.";

    private final CellulosesZLogger logger;
    private final MiniMessage miniMessage;
    private volatile RuntimeState state = new RuntimeState(
            Map.of(),
            DEFAULT_LOCALE,
            DEFAULT_FALLBACK,
            DEFAULT_PRIMARY,
            DEFAULT_SECONDARY,
            true
    );

    public DefaultMessageService(CellulosesZLogger logger) {
        this.logger = requireNonNull(logger, "logger");
        this.miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.color())
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.reset())
                        .build())
                .build();
    }

    private static String plainArgument(MessageArgument argument) {
        return switch (argument) {
            case MessageArgument.Text(var value) -> value;
            case MessageArgument.Number(var value) -> value.toPlainString();
            case MessageArgument.BooleanValue(var value) -> Boolean.toString(value);
            case MessageArgument.UuidValue(var value) -> value.toString();
            default -> throw new IllegalArgumentException(
                    "Message argument requires component rendering: "
                            + argument.getClass().getName()
            );
        };
    }

    private static Optional<MessageTemplateArguments.CompiledTemplate> lookup(
            RuntimeState state,
            String requestedLocale,
            String key
    ) {
        var requested = messages(state, requestedLocale).get(key);
        if (requested != null) {
            return Optional.of(requested);
        }

        var configured = messages(state, state.locale()).get(key);
        if (configured != null) {
            return Optional.of(configured);
        }

        return Optional.ofNullable(messages(state, state.fallback()).get(key));
    }

    public String message(String key) {
        return message(key, MessageArguments.empty());
    }

    public String message(String key, MessageArguments arguments) {
        return rich(
                state.locale(),
                key,
                arguments
        ).plainText();
    }

    public RichText rich(
            String locale,
            String key,
            MessageArguments arguments
    ) {
        return render(
                locale,
                key,
                arguments
        );
    }

    public boolean contains(String locale, String key) {
        var current = state;
        var normalized = normalizeLocaleValue(locale, current.locale());
        return messages(current, normalized).containsKey(key)
                || messages(current, current.locale()).containsKey(key)
                || messages(current, current.fallback()).containsKey(key);
    }

    private static String normalizeLocaleValue(String value, String fallbackValue) {
        return value.isBlank()
                ? fallbackValue
                : value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Map<String, MessageTemplateArguments.CompiledTemplate> messages(
            RuntimeState state,
            String requestedLocale
    ) {
        return state.locales().getOrDefault(
                normalizeLocaleValue(
                        requestedLocale,
                        state.locale()
                ),
                Map.of()
        );
    }

    public synchronized void locales(String locale, String fallback) {
        var current = state;
        var candidate = new RuntimeState(
                current.locales(),
                normalizeLocaleValue(locale, current.locale()),
                normalizeLocaleValue(fallback, current.fallback()),
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
        validateRenderedCatalogs(candidate);
        state = candidate;
    }

    private void validateRenderedCatalogs(RuntimeState candidate) {
        candidate.locales().forEach((locale, messages) ->
                messages.forEach((key, template) -> {
                    var resolver = baseResolver(candidate);
                    template.indexes()
                            .forEach(index -> resolver.resolver(Placeholder.unparsed(
                                    MessageTemplateArguments.resolverName(index),
                                    "argument"
                            )));
                    try {
                        miniMessage.deserialize(
                                candidate.legacyColors()
                                        ? LegacyMiniMessagePreprocessor.convert(template.miniMessage())
                                        : template.miniMessage(), resolver.build()
                        );
                    } catch (RuntimeException exception) {
                        throw new IllegalArgumentException(
                                "Invalid MiniMessage template for %s in %s".formatted(key, locale),
                                exception
                        );
                    }
                })
        );
    }

    private TagResolver.Builder baseResolver(RuntimeState snapshot) {
        return TagResolver.builder()
                .resolver(colorTag("primary", snapshot.primaryColor()))
                .resolver(colorTag("secondary", snapshot.secondaryColor()));
    }

    private TagResolver colorTag(String name, String color) {
        var parsed = TextColor.fromHexString(color);
        if (parsed == null) {
            throw new IllegalStateException("Invalid configured message color: " + color);
        }

        return TagResolver.resolver(name, Tag.styling(parsed));
    }

    public synchronized void theme(
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {
        var current = state;
        var candidate = new RuntimeState(
                current.locales(),
                current.locale(),
                current.fallback(),
                normalizeColor(primaryColor, DEFAULT_PRIMARY),
                normalizeColor(secondaryColor, DEFAULT_SECONDARY),
                legacyColors
        );
        validateRenderedCatalogs(candidate);
        state = candidate;
    }

    private static String normalizeColor(String value, String fallbackColor) {
        var normalized = value.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }

        return HEX.matcher(normalized).matches()
                ? normalized.toUpperCase(Locale.ROOT)
                : fallbackColor;
    }

    public void replaceCatalogs(Map<String, Map<String, String>> catalogs) {
        commitCatalogs(prepareCatalogs(catalogs));
    }

    public synchronized void commitCatalogs(PreparedMessages prepared) {
        requireNonNull(prepared, "prepared");
        var current = state;
        state = new RuntimeState(
                prepared.locales,
                current.locale(),
                current.fallback(),
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
    }

    public PreparedMessages prepareCatalogs(Map<String, Map<String, String>> catalogs) {
        requireNonNull(catalogs, "catalogs");
        try {
            var compiled = compileCatalogs(catalogs);
            validateCatalogRelations(compiled);
            var current = state;
            validateRenderedCatalogs(new RuntimeState(
                    compiled,
                    current.locale(),
                    current.fallback(),
                    current.primaryColor(),
                    current.secondaryColor(),
                    current.legacyColors()
            ));
            return new PreparedMessages(compiled);
        } catch (RuntimeException exception) {
            logger.error(
                    "Failed to prepare language catalogs; the previous catalogs remain active",
                    exception
            );
            throw new IllegalStateException(
                    "Failed to prepare language catalogs: " + exception.getMessage(),
                    exception
            );
        }
    }

    private static Map<String, Map<String, MessageTemplateArguments.CompiledTemplate>>
    compileCatalogs(
            Map<String, Map<String, String>> rawCatalogs
    ) {
        var compiled = new LinkedHashMap<String, Map<String, MessageTemplateArguments.CompiledTemplate>>();
        rawCatalogs.forEach((rawLocale, rawMessages) -> {
            var locale = normalizeLocaleValue(rawLocale, "");
            if (locale.isBlank()) {
                throw new IllegalArgumentException("Language locale must not be blank");
            }

            var messages = new LinkedHashMap<String, MessageTemplateArguments.CompiledTemplate>();
            requireNonNull(rawMessages, "catalog " + locale).forEach((key, template) -> {
                if (key.isBlank()) {
                    throw new IllegalArgumentException(
                            "Language key must not be blank in " + locale);
                }

                var previous = messages.put(
                        key,
                        MessageTemplateArguments.compile(requireNonNull(
                                template,
                                "template " + key + " in " + locale
                        ))
                );

                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Duplicate language key " + key + " in " + locale);
                }
            });
            compiled.put(locale, Map.copyOf(messages));
        });

        return Map.copyOf(compiled);
    }

    private void validateCatalogRelations(
            Map<String, Map<String, MessageTemplateArguments.CompiledTemplate>> catalogs
    ) {
        var english = catalogs.get(DEFAULT_FALLBACK);
        var chinese = catalogs.get(DEFAULT_LOCALE);

        if (english == null || chinese == null) {
            throw new IllegalArgumentException("The en_us and zh_cn language catalogs are required");
        }

        if (!english.keySet().equals(chinese.keySet())) {
            throw new IllegalArgumentException("The en_us and zh_cn language key sets differ");
        }

        catalogs.forEach((locale, messages) -> messages.forEach((key, template) -> {
            var canonical = english.get(key);
            if (canonical != null && !canonical.indexes().equals(template.indexes())) {
                throw new IllegalArgumentException(
                        "Positional argument mismatch for %s in %s: expected %s but found %s".formatted(
                                key,
                                locale,
                                canonical.indexes(),
                                template.indexes()
                        ));
            }
        }));
    }

    public MessageState snapshot() {
        var current = state;
        return new MessageState(
                current.locale(),
                current.fallback(),
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
    }

    public synchronized void commitConfiguration(
            String locale,
            String fallback,
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {
        var current = state;
        var candidate = new RuntimeState(
                current.locales(),
                normalizeLocaleValue(locale, DEFAULT_LOCALE),
                normalizeLocaleValue(fallback, DEFAULT_FALLBACK),
                normalizeColor(primaryColor, DEFAULT_PRIMARY),
                normalizeColor(secondaryColor, DEFAULT_SECONDARY),
                legacyColors
        );
        validateRenderedCatalogs(candidate);
        state = candidate;
    }

    public synchronized void restore(MessageState snapshot) {
        requireNonNull(snapshot, "snapshot");
        var current = state;
        state = new RuntimeState(
                current.locales(),
                snapshot.locale(),
                snapshot.fallback(),
                snapshot.primaryColor(),
                snapshot.secondaryColor(),
                snapshot.legacyColors()
        );
    }

    @Override
    public RichText render(String requestedLocale, String key, MessageArguments arguments) {
        requireNonNull(requestedLocale, "requestedLocale");
        requireNonNull(key, "key");
        requireNonNull(arguments, "arguments");

        return render(state, requestedLocale, key, arguments);
    }

    @Override
    public RichText renderInline(
            String requestedLocale,
            String template,
            MessageArguments arguments
    ) {
        requireNonNull(requestedLocale, "requestedLocale");
        requireNonNull(template, "template");
        requireNonNull(arguments, "arguments");

        var snapshot = state;
        try {
            return renderCompiled(
                    snapshot,
                    normalizeLocaleValue(requestedLocale, snapshot.locale()),
                    MessageTemplateArguments.compile(template),
                    arguments
            );
        } catch (RuntimeException exception) {
            logger.error("Failed to render inline message template", exception);
            return emergencyText();
        }
    }

    private RichText render(
            RuntimeState snapshot,
            String requestedLocale,
            String key,
            MessageArguments arguments
    ) {
        var normalizedLocale = normalizeLocaleValue(requestedLocale, snapshot.locale());
        var template = lookup(snapshot, normalizedLocale, key);

        if (template.isEmpty()) {
            logger.warn("Missing message key: " + key);
            template = lookup(snapshot, normalizedLocale, MISSING_KEY);
            arguments = MessageArguments.empty();
        }

        return renderSafely(
                snapshot,
                normalizedLocale,
                key,
                template.orElseGet(() -> MessageTemplateArguments.compile(EMERGENCY_MESSAGE)),
                arguments
        );
    }

    private RichText renderSafely(
            RuntimeState snapshot,
            String requestedLocale,
            String key,
            MessageTemplateArguments.CompiledTemplate template,
            MessageArguments arguments
    ) {
        try {
            if (arguments.values().size() != template.argumentCount()) {
                throw new IllegalArgumentException(
                        "Message %s requires exactly %d arguments but received %d".formatted(
                                key,
                                template.argumentCount(),
                                arguments.values().size()
                        )
                );
            }
            return renderCompiled(
                    snapshot,
                    requestedLocale,
                    template,
                    arguments
            );
        } catch (RuntimeException exception) {
            logger.error("Failed to render message key " + key, exception);
            return emergencyText();
        }
    }

    private RichText renderCompiled(
            RuntimeState snapshot,
            String requestedLocale,
            MessageTemplateArguments.CompiledTemplate template,
            MessageArguments arguments
    ) {
        if (arguments.values().size() != template.argumentCount()) {
            throw new IllegalArgumentException(
                    "Template requires exactly %d arguments but received %d".formatted(
                            template.argumentCount(),
                            arguments.values().size()
                    )
            );
        }

        var resolver = baseResolver(snapshot);
        IntStream.range(0, arguments.values().size())
                .forEach(index -> addArgument(
                        resolver,
                        snapshot,
                        requestedLocale,
                        MessageTemplateArguments.resolverName(index),
                        arguments.values().get(index)
                ));

        var input = snapshot.legacyColors()
                ? LegacyMiniMessagePreprocessor.convert(template.miniMessage())
                : template.miniMessage();

        return AdventureRichTextAdapter.fromComponent(miniMessage.deserialize(
                input,
                resolver.build()
        ));
    }

    private void addArgument(
            TagResolver.Builder resolver,
            RuntimeState snapshot,
            String requestedLocale,
            String name,
            MessageArgument argument
    ) {
        switch (argument) {
            case MessageArgument.RichTextValue(var value) ->
                    resolver.resolver(Placeholder.component(
                            name,
                            AdventureRichTextAdapter.toComponent(value)
                    ));
            case MessageArgument.NestedMessage(var message) ->
                    resolver.resolver(Placeholder.component(
                            name,
                            AdventureRichTextAdapter.toComponent(render(
                                    snapshot,
                                    requestedLocale,
                                    message.key(),
                                    message.arguments()
                            ))
                    ));
            default -> resolver.resolver(Placeholder.unparsed(
                    name,
                    plainArgument(argument)
            ));
        }
    }

    private RichText emergencyText() {
        return AdventureRichTextAdapter.fromComponent(miniMessage.deserialize(EMERGENCY_MESSAGE));
    }

    private record RuntimeState(
            Map<String, Map<String, MessageTemplateArguments.CompiledTemplate>> locales,
            String locale,
            String fallback,
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {

        private RuntimeState {
            var copied = new LinkedHashMap<String, Map<String, MessageTemplateArguments.CompiledTemplate>>();
            locales.forEach((name, messages) ->
                    copied.put(name, Map.copyOf(messages))
            );
            locales = Map.copyOf(copied);
        }

    }

    public static final class PreparedMessages {

        private final Map<String, Map<String, MessageTemplateArguments.CompiledTemplate>> locales;

        private PreparedMessages(
                Map<String, Map<String, MessageTemplateArguments.CompiledTemplate>> locales
        ) {
            var copied = new LinkedHashMap<String, Map<String, MessageTemplateArguments.CompiledTemplate>>();
            locales.forEach((name, messages) ->
                    copied.put(name, Map.copyOf(messages))
            );
            this.locales = Map.copyOf(copied);
        }

    }

    public record MessageState(
            String locale,
            String fallback,
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {

    }

}

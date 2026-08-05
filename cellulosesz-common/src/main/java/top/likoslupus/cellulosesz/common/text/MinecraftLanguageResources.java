package top.likoslupus.cellulosesz.common.text;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.core.i18n.DefaultMessageService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** Loads CellulosesZ language catalogs from Minecraft's client-resource pack stack. */
public final class MinecraftLanguageResources
        extends SimplePreparableReloadListener<DefaultMessageService.PreparedMessages> {

    private static final String NAMESPACE = "cellulosesz";
    private static final String LANGUAGE_DIRECTORY = "lang";
    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            NAMESPACE,
            "language_resources"
    );
    private static final JsonMapper STRICT_JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    private final DefaultMessageService messages;
    private final CellulosesZLogger logger;

    public MinecraftLanguageResources(
            DefaultMessageService messages,
            CellulosesZLogger logger
    ) {
        this.messages = requireNonNull(messages, "messages");
        this.logger = requireNonNull(logger, "logger");
    }

    public void register() {
        ReloadListenerRegistry.register(
                PackType.CLIENT_RESOURCES,
                this,
                LISTENER_ID
        );
    }

    /**
     * Installs the two catalogs bundled in the mod JAR as the dedicated-server fallback. Active
     * resource-pack overrides are applied only through
     * {@link #prepare(ResourceManager, ProfilerFiller)}.
     */
    public void loadBundledFallback() {
        var catalogs = new LinkedHashMap<String, Map<String, String>>();
        catalogs.put("en_us", readBundled("en_us"));
        catalogs.put("zh_cn", readBundled("zh_cn"));
        messages.replaceCatalogs(catalogs);
    }

    private Map<String, String> readBundled(String locale) {
        var path = "/assets/%s/lang/%s.json".formatted(NAMESPACE, locale);
        try (var input = MinecraftLanguageResources.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing bundled language resource " + path);
            }

            return readLanguageJson(input, path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to load bundled language fallback " + path,
                    exception
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readLanguageJson(
            InputStream input,
            String description
    ) throws IOException {
        var bytes = copy(input);
        final Map<String, Object> strict;
        try {
            strict = STRICT_JSON.readValue(bytes, Map.class);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid language JSON in " + description, exception);
        }

        var checked = new LinkedHashMap<String, String>();
        strict.forEach((key, value) -> {
            if (key.isBlank()) {
                throw new IllegalArgumentException(
                        "Blank language key in " + description
                );
            }

            if (!(value instanceof String text)) {
                throw new IllegalArgumentException(
                        "Language value for %s in %s must be a string".formatted(key, description)
                );
            }
            checked.put(key, text);
        });

        var vanilla = new LinkedHashMap<String, String>();
        Language.loadFromJson(new ByteArrayInputStream(bytes), vanilla::put);
        if (!vanilla.equals(checked)) {
            throw new IOException(
                    "Minecraft's language loader produced different data for " + description
            );
        }

        return Map.copyOf(checked);
    }

    private static byte[] copy(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        input.transferTo(output);
        return output.toByteArray();
    }

    @Override
    protected DefaultMessageService.PreparedMessages prepare(
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        var catalogs = new LinkedHashMap<String, Map<String, String>>();
        var stacks = manager.listResourceStacks(
                LANGUAGE_DIRECTORY,
                location -> location.getNamespace().equals(NAMESPACE)
                        && localeFrom(location.getPath()) != null
        );

        stacks.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    var locale = localeFrom(entry.getKey().getPath());
                    if (locale == null) {
                        return;
                    }

                    var merged = new LinkedHashMap<String, String>();
                    entry.getValue().stream()
                            .map(resource ->
                                    readResource(entry.getKey(), resource)
                            )
                            .forEach(merged::putAll);
                    catalogs.put(locale, Map.copyOf(merged));
                });

        return messages.prepareCatalogs(catalogs);
    }

    @Override
    protected void apply(
            DefaultMessageService.PreparedMessages prepared,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        messages.commitCatalogs(prepared);
        logger.info("Reloaded CellulosesZ language resources from the active resource-pack stack.");
    }

    private static @Nullable String localeFrom(String path) {
        if (!path.startsWith(LANGUAGE_DIRECTORY + "/")
                || !path.endsWith(".json")
        ) {
            return null;
        }

        var locale = path.substring(LANGUAGE_DIRECTORY.length() + 1, path.length() - 5)
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
        return locale.isBlank() || locale.indexOf('/') >= 0
                ? null
                : locale;
    }

    private Map<String, String> readResource(Identifier location, Resource resource) {
        try (var input = resource.open()) {
            return readLanguageJson(
                    input,
                    location + " from pack " + resource.sourcePackId()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read language resource " + location
                            + " from pack " + resource.sourcePackId(),
                    exception
            );
        }
    }

}

package top.likoslupus.cellulosesz.modules.economy.service;

import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.economy.data.WorthDocument;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class JsonWorthService implements WorthService {

    private final StorageService storage;
    private final Path path;
    private final WorthDocument document;

    public JsonWorthService(StorageService storage, Path directory) {
        this.storage = storage;
        this.path = directory.resolve("worth.json");
        this.document = storage.load(path, WorthDocument.class, WorthDocument::new).join();
    }

    @Override
    public synchronized Optional<BigDecimal> worth(String itemId) {
        var value = document.prices.get(normalize(itemId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public synchronized void setWorth(String itemId, BigDecimal amount) {
        document.prices.put(normalize(itemId), amount.toPlainString());
        save();
    }

    @Override
    public synchronized void removeWorth(String itemId) {
        document.prices.remove(normalize(itemId));
        save();
    }

    @Override
    public synchronized Map<String, BigDecimal> allWorths() {
        var result = new LinkedHashMap<String, BigDecimal>();
        document.prices.forEach((item, value) -> {
            try {
                result.put(item, new BigDecimal(value));
            } catch (NumberFormatException _) {
                // skip malformed values and let the next save normalize the document
            }
        });
        return result;
    }

    private void save() {
        storage.save(path, document);
    }

    private String normalize(String itemId) {
        var value = itemId.trim().toLowerCase();
        return value.indexOf(':') < 0 ? "minecraft:" + value : value;
    }

}

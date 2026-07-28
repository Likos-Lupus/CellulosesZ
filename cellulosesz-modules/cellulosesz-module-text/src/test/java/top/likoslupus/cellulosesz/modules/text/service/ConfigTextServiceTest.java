package top.likoslupus.cellulosesz.modules.text.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.modules.text.config.TextConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ConfigTextServiceTest {

    @Test
    void snapshotIsDefensiveAndCaseNormalized() {
        var config = new TextConfig();
        var lines = new ArrayList<>(List.of("first"));
        config.custom = new LinkedHashMap<>();
        config.custom.put(" Rules-Extra ", lines);
        var service = new ConfigTextService(config);

        lines.add("mutated");
        config.custom.clear();

        assertEquals(List.of("first"), service.custom("rules-extra"));
        assertEquals(java.util.Set.of("rules-extra"), service.customNames());
        assertThrows(UnsupportedOperationException.class,
                () -> service.custom("rules-extra").add("illegal"));
    }

    @Test
    void invalidReloadKeepsPreviousSnapshot() {
        var valid = new TextConfig();
        valid.info = List.of("stable");
        var service = new ConfigTextService(valid);
        var invalid = new TextConfig();
        invalid.pageSize = 0;
        invalid.info = List.of("replacement");

        assertThrows(IllegalArgumentException.class, () -> service.configure(invalid));
        assertEquals(List.of("stable"), service.info());
    }

}

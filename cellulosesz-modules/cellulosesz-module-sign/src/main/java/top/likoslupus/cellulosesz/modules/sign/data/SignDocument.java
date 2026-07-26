package top.likoslupus.cellulosesz.modules.sign.data;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SignDocument {

    public Map<String, StoredSign> signs = new LinkedHashMap<>();

    public void validate() {
        var validated = new LinkedHashMap<String, StoredSign>();
        signs.forEach((key, value) -> {
            if (key.isBlank()) throw new IllegalArgumentException("Sign key must not be blank");
            value.validate();
            validated.put(key, value);
        });
        signs = validated;
    }

    public SignDocument copy() {
        var copy = new SignDocument();
        signs.forEach((key, value) ->
                copy.signs.put(key, value.copy())
        );
        return copy;
    }

}

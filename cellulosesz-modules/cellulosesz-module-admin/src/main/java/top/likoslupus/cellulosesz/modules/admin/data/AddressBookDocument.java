package top.likoslupus.cellulosesz.modules.admin.data;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AddressBookDocument {

    public Map<String, Entry> players = new LinkedHashMap<>();

    public static final class Entry {

        public String name = "";
        public String address = "";

    }

}

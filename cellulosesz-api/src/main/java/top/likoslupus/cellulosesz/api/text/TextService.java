package top.likoslupus.cellulosesz.api.text;

import java.util.List;
import java.util.Set;

public interface TextService {

    List<String> info();

    List<String> motd();

    List<String> rules();

    List<String> custom(String name);

    Set<String> customNames();

    int pageSize();

}

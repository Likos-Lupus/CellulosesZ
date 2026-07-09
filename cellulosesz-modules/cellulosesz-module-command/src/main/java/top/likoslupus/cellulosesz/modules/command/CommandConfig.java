package top.likoslupus.cellulosesz.modules.command;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CommandConfig {

    public int schema = 1;
    public Set<String> disabledCommands = new LinkedHashSet<>();
    public boolean auditCommands = true;

}

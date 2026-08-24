package top.likoslupus.cellulosesz.core.command.service;

import java.util.Collection;
import java.util.Set;

/**
 * Atomically published availability snapshot for canonical command roots.
 */
public interface CommandAvailabilityService {

    boolean disabled(String canonicalRoot);

    Set<String> disabledCommands();

    void replaceDisabledCommands(Collection<String> canonicalRoots);

}

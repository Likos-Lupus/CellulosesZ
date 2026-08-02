package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.CommandAvailabilityService;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultCommandAvailabilityService implements CommandAvailabilityService {

    private final AtomicReference<Set<String>> disabled = new AtomicReference<>(Set.of());

    @Override
    public boolean disabled(String canonicalRoot) {
        return disabled.get().contains(normalize(canonicalRoot));
    }

    @Override
    public Set<String> disabledCommands() {
        return disabled.get();
    }

    @Override
    public void replaceDisabledCommands(Collection<String> canonicalRoots) {
        requireNonNull(canonicalRoots, "canonicalRoots");
        var replacement = new LinkedHashSet<String>();
        canonicalRoots.stream()
                .map(this::normalize)
                .forEach(replacement::add);
        disabled.set(Set.copyOf(replacement));
    }

    private String normalize(String value) {
        return requireNonBlank(value, "canonicalRoot")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

}

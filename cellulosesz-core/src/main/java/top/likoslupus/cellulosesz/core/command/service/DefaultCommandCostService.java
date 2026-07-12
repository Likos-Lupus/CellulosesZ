package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultCommandCostService implements CommandCostService {

    private final ServiceRegistry services;
    private final Map<String, BigDecimal> costs = new ConcurrentHashMap<>();

    public DefaultCommandCostService(ServiceRegistry services) {
        this.services = services;
    }

    public void configure(Map<String, BigDecimal> configured) {
        costs.clear();
        configured.forEach((key, value) -> {
            if (value.signum() > 0) costs.put(normalize(key), value);
        });
    }

    private String normalize(String command) {
        return command.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public BigDecimal cost(String command) {
        return costs.getOrDefault(normalize(command), BigDecimal.ZERO);
    }

    @Override
    public boolean charge(UUID uuid, String command) {
        var amount = cost(command);
        if (amount.signum() <= 0) return true;
        return services.optional(EconomyService.class)
                .map(economy ->
                        economy.withdraw(
                                uuid,
                                amount,
                                TransactionCause.command("cellulosesz", command)
                        ).success()
                )
                .orElse(false);
    }

}

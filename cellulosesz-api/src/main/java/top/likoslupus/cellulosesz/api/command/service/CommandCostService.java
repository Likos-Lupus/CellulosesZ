package top.likoslupus.cellulosesz.api.command.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CommandCostService {

    BigDecimal cost(String command);

    CompletableFuture<Boolean> charge(UUID uuid, String command);

}

package top.likoslupus.cellulosesz.core.command.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CommandCostService {

    BigDecimal cost(String command);

    CompletionStage<CommandCostReserveResult> reserve(UUID uuid, String command);

}

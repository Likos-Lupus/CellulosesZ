package top.likoslupus.cellulosesz.api.command.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface CommandCostService {

    BigDecimal cost(String command);

    boolean charge(UUID uuid, String command);

}

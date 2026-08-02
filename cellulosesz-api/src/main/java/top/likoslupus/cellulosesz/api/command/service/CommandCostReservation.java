package top.likoslupus.cellulosesz.api.command.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

public interface CommandCostReservation {

    BigDecimal amount();

    CompletionStage<Void> commit();

    CompletionStage<Void> refund();

}

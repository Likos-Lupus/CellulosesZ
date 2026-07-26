package top.likoslupus.cellulosesz.api.economy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface WorthService {

    Optional<BigDecimal> worth(String itemId);

    CompletableFuture<Void> setWorth(String itemId, BigDecimal amount);

    CompletableFuture<Boolean> removeWorth(String itemId);

    Map<String, BigDecimal> allWorths();

}

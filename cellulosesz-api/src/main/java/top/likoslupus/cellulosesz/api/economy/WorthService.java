package top.likoslupus.cellulosesz.api.economy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface WorthService {

    Optional<BigDecimal> worth(String itemId);

    void setWorth(String itemId, BigDecimal amount);

    void removeWorth(String itemId);

    Map<String, BigDecimal> allWorths();

}

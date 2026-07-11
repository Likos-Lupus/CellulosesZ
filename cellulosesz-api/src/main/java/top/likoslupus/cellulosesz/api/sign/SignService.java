package top.likoslupus.cellulosesz.api.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;

public interface SignService {

    void register(CellSignHandler handler);

    List<String> handlers();

    SignUseResult use(
            CellPlayer player,
            List<String> lines,
            boolean sneaking
    );

}

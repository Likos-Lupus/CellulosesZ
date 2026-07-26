package top.likoslupus.cellulosesz.api.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;

public interface SignService {

    void register(CellSignHandler handler);

    List<String> handlers();

    List<String> formattedLines(List<String> lines);

    SignMutationExecution create(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> lines
    );

    SignMutationExecution edit(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> previousLines,
            List<String> lines
    );

    SignMutationExecution breakSign(
            CellPlayer player,
            CellLocation location,
            List<String> frontLines,
            List<String> backLines
    );

    SignExecution use(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> lines,
            boolean sneaking
    );

}

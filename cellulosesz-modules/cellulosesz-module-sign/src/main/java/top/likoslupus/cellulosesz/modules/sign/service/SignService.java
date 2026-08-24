package top.likoslupus.cellulosesz.modules.sign.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.sign.domain.SignExecution;
import top.likoslupus.cellulosesz.modules.sign.domain.SignMutationExecution;
import top.likoslupus.cellulosesz.modules.sign.handler.CellSignHandler;

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

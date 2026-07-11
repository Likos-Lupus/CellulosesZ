package top.likoslupus.cellulosesz.api.sign;

public interface CellSignHandler {

    String id();

    SignUseResult use(SignUseContext context);

}

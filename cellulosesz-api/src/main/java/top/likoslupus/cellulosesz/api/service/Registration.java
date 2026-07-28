package top.likoslupus.cellulosesz.api.service;

public interface Registration extends AutoCloseable {

    String owner();

    boolean closed();

    @Override
    void close();

}

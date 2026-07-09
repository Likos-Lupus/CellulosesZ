package top.likoslupus.cellulosesz.api.logging;

public interface CellulosesZLogger {

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);

    default void debug(String message) {
        info(message);
    }

    void info(String message);

}

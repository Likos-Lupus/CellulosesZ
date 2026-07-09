package top.likoslupus.cellulosesz.fabric;

import org.slf4j.Logger;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

public final class Slf4jCellulosesZLogger implements CellulosesZLogger {

    private final Logger logger;

    public Slf4jCellulosesZLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

}

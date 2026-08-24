package top.likoslupus.cellulosesz.core.scheduler;

import top.likoslupus.cellulosesz.api.service.Registration;

public interface TaskHandle extends Registration {

    boolean cancelled();

    @Override
    default void close() {
        cancel();
    }

    void cancel();

}

package top.likoslupus.cellulosesz.api.scheduler;

import top.likoslupus.cellulosesz.api.service.Registration;

public interface TaskHandle extends Registration {

    void cancel();

    boolean cancelled();

    @Override
    default void close() {
        cancel();
    }

}

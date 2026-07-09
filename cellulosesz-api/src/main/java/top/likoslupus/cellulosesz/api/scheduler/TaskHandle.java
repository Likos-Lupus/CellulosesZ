package top.likoslupus.cellulosesz.api.scheduler;

public interface TaskHandle {

    void cancel();

    boolean cancelled();

}

package top.likoslupus.cellulosesz.core.module;

public final class ModuleLoadException extends RuntimeException {

    public ModuleLoadException(String message) {
        super(message);
    }

    public ModuleLoadException(String message, Throwable cause) {
        super(message, cause);
    }

}

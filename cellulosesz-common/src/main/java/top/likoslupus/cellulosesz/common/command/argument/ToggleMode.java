package top.likoslupus.cellulosesz.common.command.argument;

public enum ToggleMode {

    ON(true),
    OFF(false);

    private final boolean enabled;

    ToggleMode(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

}

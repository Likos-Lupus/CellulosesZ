package top.likoslupus.cellulosesz.modules.playerstate.command;

final class PlayerTimeFormat {

    private PlayerTimeFormat() {
    }

    static String duration(long millis) {
        var seconds = Math.max(0L, millis / 1000L);
        var days = seconds / 86_400L;
        var hours = seconds % 86_400L / 3_600L;
        var minutes = seconds % 3_600L / 60L;
        var remaining = seconds % 60L;
        return "%dd %02dh %02dm %02ds".formatted(days, hours, minutes, remaining);
    }

}

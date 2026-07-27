package top.likoslupus.cellulosesz.modules.sign;

import java.util.Locale;

public final class SignConfig {

    public boolean enabled = true;
    public int editTargetDistance = 20;
    public int editMaximumLineLength = 384;
    public Interaction interaction = new Interaction();
    public Signs signs = new Signs();

    public void validate() {
        if (editTargetDistance < 1 || editTargetDistance > 128) {
            throw new IllegalArgumentException("editTargetDistance must be between 1 and 128");
        }
        if (editMaximumLineLength < 1 || editMaximumLineLength > 4096) {
            throw new IllegalArgumentException("editMaximumLineLength must be between 1 and 4096");
        }
        if (interaction == null || interaction.cooldownTicks < 0 || interaction.cooldownTicks > 1200) {
            throw new IllegalArgumentException("interaction.cooldownTicks must be between 0 and 1200");
        }
        if (signs == null) throw new IllegalArgumentException("signs must not be null");
    }

    public static final class Interaction {

        public int cooldownTicks = 10;

    }

    public static final class Signs {

        public boolean warp = true;
        public boolean buy = true;
        public boolean sell = true;
        public boolean kit = true;
        public boolean balance = true;
        public boolean free = true;
        public boolean trade = true;
        public boolean enchant = true;
        public boolean repair = true;
        public boolean gamemode = true;
        public boolean heal = true;
        public boolean info = true;
        public boolean mail = true;
        public boolean randomteleport = true;
        public boolean anvil = true;
        public boolean cartography = true;
        public boolean disposal = true;
        public boolean grindstone = true;
        public boolean loom = true;
        public boolean smithing = true;
        public boolean workbench = true;
        public boolean spawnmob = true;
        public boolean time = true;
        public boolean weather = true;

        public boolean enabled(String id) {
            return switch (id.toLowerCase(Locale.ROOT)) {
                case "warp" -> warp;
                case "buy" -> buy;
                case "sell" -> sell;
                case "kit" -> kit;
                case "balance" -> balance;
                case "free" -> free;
                case "trade" -> trade;
                case "enchant" -> enchant;
                case "repair" -> repair;
                case "gamemode" -> gamemode;
                case "heal" -> heal;
                case "info" -> info;
                case "mail" -> mail;
                case "randomteleport" -> randomteleport;
                case "anvil" -> anvil;
                case "cartography" -> cartography;
                case "disposal" -> disposal;
                case "grindstone" -> grindstone;
                case "loom" -> loom;
                case "smithing" -> smithing;
                case "workbench" -> workbench;
                case "spawnmob" -> spawnmob;
                case "time" -> time;
                case "weather" -> weather;
                default -> false;
            };
        }

    }

}

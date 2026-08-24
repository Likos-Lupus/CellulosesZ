package top.likoslupus.cellulosesz.modules.sign;

import java.util.Locale;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;

import static java.util.Objects.requireNonNull;

public final class SignConfig {

    public boolean enabled = true;
    public int editTargetDistance = 20;
    public int editMaximumLineLength = 384;
    public Interaction interaction = new Interaction();
    public Signs signs = new Signs();

    public SignConfig validatedCopy() {
        var copy = new SignConfig();
        copy.copyFrom(this);
        copy.validate();
        return copy;
    }

    public void copyFrom(SignConfig source) {
        requireNonNull(source, "source").validate();
        enabled = source.enabled;
        editTargetDistance = source.editTargetDistance;
        editMaximumLineLength = source.editMaximumLineLength;
        interaction = new Interaction();
        interaction.cooldownTicks = source.interaction.cooldownTicks;
        signs = new Signs();
        signs.warp = source.signs.warp;
        signs.buy = source.signs.buy;
        signs.sell = source.signs.sell;
        signs.kit = source.signs.kit;
        signs.balance = source.signs.balance;
        signs.free = source.signs.free;
        signs.trade = source.signs.trade;
        signs.enchant = source.signs.enchant;
        signs.repair = source.signs.repair;
        signs.gamemode = source.signs.gamemode;
        signs.heal = source.signs.heal;
        signs.info = source.signs.info;
        signs.mail = source.signs.mail;
        signs.randomteleport = source.signs.randomteleport;
        signs.anvil = source.signs.anvil;
        signs.cartography = source.signs.cartography;
        signs.disposal = source.signs.disposal;
        signs.grindstone = source.signs.grindstone;
        signs.loom = source.signs.loom;
        signs.smithing = source.signs.smithing;
        signs.workbench = source.signs.workbench;
        signs.spawnmob = source.signs.spawnmob;
        signs.time = source.signs.time;
        signs.weather = source.signs.weather;
    }

    public void validate() {
        requireInRange(
                editTargetDistance,
                1,
                128,
                "editTargetDistance"
        );
        requireInRange(
                editMaximumLineLength,
                1,
                4_096,
                "editMaximumLineLength"
        );
        requireNonNull(
                interaction,
                "interaction"
        );
        requireInRange(
                interaction.cooldownTicks,
                0,
                1_200,
                "interaction.cooldownTicks"
        );
        requireNonNull(
                signs,
                "signs"
        );
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

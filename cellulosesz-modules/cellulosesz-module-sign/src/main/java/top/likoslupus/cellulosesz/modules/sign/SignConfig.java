package top.likoslupus.cellulosesz.modules.sign;

public final class SignConfig {

    public boolean enabled = true;
    public Interaction interaction = new Interaction();
    public Signs signs = new Signs();

    public static final class Interaction {

        public int cooldownTicks = 10;

    }

    public static final class Signs {

        public boolean warp = true;
        public boolean buy = true;
        public boolean sell = true;
        public boolean kit = true;

    }

}

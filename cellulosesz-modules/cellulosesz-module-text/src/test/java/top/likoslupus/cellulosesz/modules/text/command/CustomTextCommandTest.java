package top.likoslupus.cellulosesz.modules.text.command;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.text.TextService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class CustomTextCommandTest {

    @Test
    void pageSelectsOnlyTheRequestedSlice() {
        var invocation = new TestInvocation("guide", "2");
        var command = new CustomTextCommand(new FixedTextService());

        assertEquals(1, command.execute(invocation));
        assertEquals(List.of("line-3"), invocation.lines);
        assertEquals(2, invocation.titlePlaceholders.get("page"));
        assertEquals(2, invocation.titlePlaceholders.get("pages"));
    }

    @Test
    void rejectsExtraArgumentsAndNonPositivePages() {
        var command = new CustomTextCommand(new FixedTextService());
        var extra = new TestInvocation("guide", "1", "extra");
        var zero = new TestInvocation("guide", "0");

        assertEquals(0, command.execute(extra));
        assertEquals("commands.text.custom-usage", extra.errorKey);
        assertEquals(0, command.execute(zero));
        assertEquals("commands.common.invalid-page", zero.errorKey);
    }

    private static final class FixedTextService implements TextService {

        @Override
        public List<String> info() {
            return List.of();
        }

        @Override
        public List<String> motd() {
            return List.of();
        }

        @Override
        public List<String> rules() {
            return List.of();
        }

        @Override
        public List<String> custom(String name) {
            return name.equalsIgnoreCase("guide") ? List.of("line-1", "line-2", "line-3") : List.of();
        }

        @Override
        public Set<String> customNames() {
            return Set.of("guide");
        }

        @Override
        public int pageSize() {
            return 2;
        }

    }

    private static final class TestInvocation implements CommandInvocation {

        private final String[] args;
        private final List<String> lines = new ArrayList<>();
        private String errorKey = "";
        private Map<String, ?> titlePlaceholders = Map.of();

        private TestInvocation(String... args) {
            this.args = args;
        }

        @Override
        public Object nativeSource() {
            return this;
        }

        @Override
        public String label() {
            return "customtext";
        }

        @Override
        public String[] args() {
            return args.clone();
        }

        @Override
        public boolean player() {
            return true;
        }

        @Override
        public Optional<String> playerName() {
            return Optional.of("tester");
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public ResolvedPlayer resolvePlayer(String input) {
            return new ResolvedPlayer(ResolvedPlayerState.UNKNOWN, null, input, null, false);
        }

        @Override
        public String locale() {
            return "en";
        }

        @Override
        public void reply(String message) {
        }

        @Override
        public void reply(RichText message) {
        }

        @Override
        public void replyKey(String key, Map<String, ?> placeholders) {
            if (key.equals("commands.text.line")) lines.add(String.valueOf(placeholders.get("line")));
            else titlePlaceholders = Map.copyOf(placeholders);
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(RichText message) {
        }

        @Override
        public void errorKey(String key, Map<String, ?> placeholders) {
            errorKey = key;
        }

    }

}

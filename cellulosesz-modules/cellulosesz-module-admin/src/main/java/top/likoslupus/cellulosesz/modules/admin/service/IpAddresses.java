package top.likoslupus.cellulosesz.modules.admin.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

public final class IpAddresses {

    private IpAddresses() {
    }

    /**
     * Parses numeric literals only. Host names and IPv6 zone identifiers are rejected before JDK parsing.
     */
    public static Optional<InetAddress> parseLiteral(String input) {
        var value = input.trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.contains("%") || value.contains("/")) {
            return Optional.empty();
        }
        if (!(ipv4Shape(value) || ipv6Shape(value))) {
            return Optional.empty();
        }

        try {
            // shape checks above prevent DNS host names
            var parsed = InetAddress.getByName(value);
            var bytes = parsed.getAddress();

            if (ipv4Mapped(bytes)) {
                return Optional.of(InetAddress.getByAddress(
                        new byte[]{
                                bytes[12],
                                bytes[13],
                                bytes[14],
                                bytes[15]
                        }
                ));
            }

            return Optional.of(parsed);
        } catch (UnknownHostException _) {
            return Optional.empty();
        }
    }

    private static boolean ipv4Shape(String value) {
        if (!value.matches("[0-9.]+")) {
            return false;
        }

        var parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }

        for (var part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }

            try {
                var number = Integer.parseInt(part);
                if (number < 0 || number > 255) {
                    return false;
                }
            } catch (NumberFormatException _) {
                return false;
            }
        }

        return true;
    }

    private static boolean ipv6Shape(String value) {
        return value.contains(":")
                && value.matches("[0-9A-Fa-f:.]+")
                && value.chars().filter(ch -> ch == ':').count() >= 2;
    }

    private static boolean ipv4Mapped(byte[] bytes) {
        if (bytes.length != 16) {
            return false;
        }

        return IntStream.range(0, 10).noneMatch(index -> bytes[index] != 0)
                && ((bytes[10] & 255) == 255
                && (bytes[11] & 255) == 255);
    }

    public static String canonical(InetAddress address) {
        var bytes = address.getAddress();
        if (ipv4Mapped(bytes)) {
            return "%d.%d.%d.%d".formatted(
                    bytes[12] & 255,
                    bytes[13] & 255,
                    bytes[14] & 255,
                    bytes[15] & 255
            );
        }

        return address.getHostAddress().toLowerCase(Locale.ROOT);
    }

}

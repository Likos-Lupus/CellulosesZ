package top.likoslupus.cellulosesz.modules.admin.service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;

public final class IpAddresses {

    private IpAddresses() {
    }

    public static Optional<String> normalize(String input) {
        var value = input.trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        if (!(ipv4Shape(value) || ipv6Shape(value))) return Optional.empty();

        try {
            var address = InetAddress.getByName(value);
            if (address instanceof Inet6Address ipv6) {
                var bytes = ipv6.getAddress();
                if (ipv4Mapped(bytes)) {
                    return Optional.of("%d.%d.%d.%d".formatted(
                            bytes[12] & 0xff, bytes[13] & 0xff, bytes[14] & 0xff, bytes[15] & 0xff
                    ));
                }
                var normalized = ipv6.getHostAddress().toLowerCase(Locale.ROOT);
                var zone = normalized.indexOf('%');
                return Optional.of(zone < 0 ? normalized : normalized.substring(0, zone));
            }
            if (ipv4Shape(value)) {
                return Optional.of(address.getHostAddress().toLowerCase(Locale.ROOT));
            }
        } catch (UnknownHostException _) {
        }

        return Optional.empty();
    }

    private static boolean ipv4Shape(String value) {
        if (!value.matches("[0-9.]+")) return false;

        var parts = value.split("\\.", -1);
        if (parts.length != 4) return false;

        for (var part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;

            int number;
            try {
                number = Integer.parseInt(part);
            } catch (NumberFormatException _) {
                return false;
            }
            if (number < 0 || number > 255) return false;
        }

        return true;
    }

    private static boolean ipv6Shape(String value) {
        return value.contains(":")
                && value.matches("[0-9A-Fa-f:.%]+")
                && !value.contains("/");
    }

    private static boolean ipv4Mapped(byte[] bytes) {
        if (bytes.length != 16) return false;
        for (var index = 0; index < 10; index++) {
            if (bytes[index] != 0) return false;
        }
        return (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff;
    }

}

package project.smartbanking.util;

public final class IdentifierUtils {

    private IdentifierUtils() {
    }

    // +84912345678 -> 0912345678
    public static String normalizePhone(String phone) {
        String p = phone.trim().replace(" ", "");
        if (p.startsWith("+84")) {
            p = "0" + p.substring(3);
        }
        return p;
    }

    // Có dấu @ thì là email (chữ thường), ngược lại là số điện thoại
    public static String normalizeIdentifier(String identifier) {
        String value = identifier.trim();
        return value.contains("@") ? value.toLowerCase() : normalizePhone(value);
    }
}
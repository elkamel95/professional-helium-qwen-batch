package com.example.productai.util;

public final class NumberUtils {

    private NumberUtils() {
    }

    public static Integer toInteger(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : Integer.valueOf(normalized);
    }

    public static Double toDouble(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : Double.valueOf(normalized);
    }

    public static boolean toBooleanFlag(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim()
                .replace("\"", "")
                .toLowerCase();

        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "y".equals(normalized);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace("\"", "")
                .replace(",", "")
                .replace(" ", "");

        if (normalized.isBlank()
                || "-".equals(normalized)
                || "n/a".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)) {
            return null;
        }

        return normalized;
    }
}
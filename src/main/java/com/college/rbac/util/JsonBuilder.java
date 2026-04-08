package com.college.rbac.util;

import java.util.*;

/**
 * Simple JSON builder utility — no external dependencies needed.
 */
public class JsonBuilder {

    public static String string(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\"";
    }

    public static String object(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append(string(entry.getKey())).append(":").append(toJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static String array(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            sb.append(toJson(item));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return string(s);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> m) return object((Map<String, Object>) m);
        if (value instanceof List<?> l) return array(l);
        return string(value.toString());
    }

    /**
     * Parse simple key=value form body (URL encoded).
     */
    public static Map<String, String> parseFormBody(String body) {
        Map<String, String> params = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return params;
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(urlDecode(kv[0].trim()), urlDecode(kv[1].trim()));
            }
        }
        return params;
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}

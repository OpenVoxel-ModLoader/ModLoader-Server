package com.happyuky7.openvoxelmoding.server.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Small YAML subset for the ModLoader server files: {@code key: value}, nested sections
 * (keys become {@code section.key}) and {@code - item} lists under a key.
 */
final class Yaml {
    final Map<String, String> values = new LinkedHashMap<>();
    final Map<String, List<String>> lists = new LinkedHashMap<>();

    static Yaml load(Path path) {
        Yaml y = new Yaml();
        if (!Files.isRegularFile(path)) return y;
        try {
            Deque<int[]> indents = new ArrayDeque<>();         // indent of each open section
            Deque<String> names = new ArrayDeque<>();
            String listKey = null;
            for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = stripComment(raw);
                if (line.trim().isEmpty()) continue;
                int indent = line.length() - line.replaceAll("^\\s+", "").length();
                String s = line.trim();
                if (s.startsWith("- ") || s.equals("-")) {
                    if (listKey != null) y.lists.computeIfAbsent(listKey, k -> new ArrayList<>()).add(unquote(s.length() > 1 ? s.substring(2).trim() : ""));
                    continue;
                }
                while (!indents.isEmpty() && indents.peek()[0] >= indent) { indents.pop(); names.pop(); }
                int at = s.indexOf(':');
                if (at < 0) continue;
                String key = s.substring(0, at).trim(), value = s.substring(at + 1).trim();
                StringBuilder full = new StringBuilder();
                List<String> path0 = new ArrayList<>(names); Collections.reverse(path0);
                for (String n : path0) full.append(n).append('.');
                full.append(key);
                if (value.isEmpty()) {
                    indents.push(new int[]{indent}); names.push(key);
                    listKey = full.toString();
                } else if (value.equals("[]")) { y.lists.put(full.toString(), new ArrayList<>()); listKey = null; }
                else { y.values.put(full.toString(), unquote(value)); listKey = null; }
            }
        } catch (Exception error) { System.err.println("[ModLoader] yaml " + path + ": " + error); }
        return y;
    }

    private static String stripComment(String raw) {
        boolean single = false, dbl = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == '#' && !single && !dbl && (i == 0 || Character.isWhitespace(raw.charAt(i - 1)))) return raw.substring(0, i);
        }
        return raw;
    }

    private static String unquote(String v) {
        if (v.length() >= 2 && v.startsWith("'") && v.endsWith("'")) return v.substring(1, v.length() - 1).replace("''", "'");
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) return v.substring(1, v.length() - 1);
        return v;
    }

    String get(String key, String def) { return values.getOrDefault(key, def); }
    int getInt(String key, int def) { try { return Integer.parseInt(get(key, "").trim()); } catch (Exception e) { return def; } }
    boolean getBool(String key, boolean def) { String v = get(key, null); return v == null ? def : v.trim().equalsIgnoreCase("true"); }
    List<String> list(String key) { return lists.getOrDefault(key, Collections.emptyList()); }

    static String quote(String v) { return "'" + v.replace("'", "''") + "'"; }

    /** Writes the file only if it does not exist yet (defaults with comments). */
    static void writeDefault(Path path, String content) {
        try { if (!Files.exists(path)) { Files.createDirectories(path.toAbsolutePath().getParent()); Files.writeString(path, content, StandardCharsets.UTF_8); } }
        catch (Exception error) { System.err.println("[ModLoader] cannot write " + path + ": " + error); }
    }
}

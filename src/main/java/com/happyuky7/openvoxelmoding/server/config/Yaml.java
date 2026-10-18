package com.happyuky7.openvoxelmoding.server.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Small YAML subset for the ModLoader server files: {@code key: value}, nested sections
 * (keys become {@code section.key}) and {@code - item} lists under a key.
 */
public final class Yaml {
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, List<String>> lists = new LinkedHashMap<>();

    /** Reads a file; a missing or unreadable file gives an empty Yaml. */
    public static Yaml load(Path path) {
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

    public String get(String key, String def) { return values.getOrDefault(key, def); }
    public int getInt(String key, int def) { try { return Integer.parseInt(get(key, "").trim()); } catch (Exception e) { return def; } }
    public boolean getBool(String key, boolean def) { String v = get(key, null); return v == null ? def : v.trim().equalsIgnoreCase("true"); }
    /** All scalar values with their full dotted keys. */
    public Map<String, String> values() { return Collections.unmodifiableMap(values); }
    /** true if the key exists as a value or a list. */
    public boolean has(String key) { return values.containsKey(key) || lists.containsKey(key); }
    public List<String> list(String key) { return lists.getOrDefault(key, Collections.emptyList()); }

    public static String quote(String v) { return "'" + v.replace("'", "''") + "'"; }

    /** Writes the file only if it does not exist yet (defaults with comments). */
    public static void writeDefault(Path path, String content) {
        try { if (!Files.exists(path)) { Files.createDirectories(path.toAbsolutePath().getParent()); Files.writeString(path, content, StandardCharsets.UTF_8); } }
        catch (Exception error) { System.err.println("[ModLoader] cannot write " + path + ": " + error); }
    }
}

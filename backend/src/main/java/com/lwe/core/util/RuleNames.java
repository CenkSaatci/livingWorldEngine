package com.lwe.core.util;

import java.util.List;
import java.util.Map;

/**
 * ADR-014: Namen aus Regel-Content (Skills, Attribute, Zustände, Merkmale)
 * werden beim Lookup einheitlich <b>case-insensitiv</b> verglichen.
 * Merkmal-Tier-Suffixe ("Zauberer II") zählen weiter als Treffer für "Zauberer".
 */
public final class RuleNames {

    private RuleNames() {}

    public static boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    public static <V> V get(Map<String, V> map, String key) {
        if (map == null || key == null) return null;
        if (map.containsKey(key)) return map.get(key);
        for (var e : map.entrySet()) {
            if (eq(e.getKey(), key)) return e.getValue();
        }
        return null;
    }

    public static <V> V getOr(Map<String, V> map, String key, V fallback) {
        var value = get(map, key);
        return value != null ? value : fallback;
    }

    /** Merkmalsname inkl. Tier-Suffix ("Hohe Lebenskraft III" erfüllt "Hohe Lebenskraft"). */
    public static boolean hasTrait(List<String> selected, String name) {
        if (selected == null || name == null) return false;
        for (var s : selected) {
            if (s == null) continue;
            if (s.equalsIgnoreCase(name)) return true;
            if (s.length() > name.length()
                && s.regionMatches(true, 0, name, 0, name.length())
                && s.charAt(name.length()) == ' ') return true;
        }
        return false;
    }
}

package model;

import java.util.*;

/**
 * Flexible operation counter. Stores counts by string keys.
 * Example keys: "dfs_visits", "edges_traversed", "relaxations", "kahn_pops"
 */
public class OperationCounter {
    private final Map<String, Long> counters = new LinkedHashMap<>();

    public void inc(String key) {
        counters.merge(key, 1L, Long::sum);
    }

    public void add(String key, long delta) {
        counters.merge(key, delta, Long::sum);
    }

    public long get(String key) {
        return counters.getOrDefault(key, 0L);
    }

    public Map<String, Long> getAll() {
        return Collections.unmodifiableMap(counters);
    }

    @Override
    public String toString() {
        return counters.toString();
    }
}

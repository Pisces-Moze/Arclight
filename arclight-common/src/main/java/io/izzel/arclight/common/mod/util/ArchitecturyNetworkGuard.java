package io.izzel.arclight.common.mod.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collection factories used by the Architectury networking compatibility layer.
 * Receiver and transformer tables are read for every Architectury packet, so their steady-state
 * path stays lock-free. The per-player multimap uses a concurrent map and an independent
 * concurrent set for each player, avoiding a global lock on channel capability checks.
 */
public final class ArchitecturyNetworkGuard {

    private ArchitecturyNetworkGuard() {
    }

    public static <K, V> Map<K, V> concurrentMap(Map<K, V> source) {
        return new ConcurrentHashMap<>(source);
    }

    public static <E> Set<E> concurrentSet(Set<E> source) {
        Set<E> result = ConcurrentHashMap.newKeySet(Math.max(source.size(), 16));
        result.addAll(source);
        return result;
    }

    public static <K, V> SetMultimap<K, V> concurrentSetMultimap(Multimap<K, V> source) {
        Map<K, Collection<V>> backingMap = new ConcurrentHashMap<>();
        SetMultimap<K, V> result = Multimaps.newSetMultimap(backingMap, ConcurrentHashMap::newKeySet);
        result.putAll(source);
        return result;
    }
}

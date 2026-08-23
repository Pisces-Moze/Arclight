package io.izzel.arclight.common.mod.util;

import com.google.common.collect.HashMultimap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitecturyNetworkGuardTest {

    @Test
    void preservesReceiverAndTransformerMapSemantics() {
        Map<String, String> original = new HashMap<>();
        original.put("architectury:sync_ids", "initial");

        Map<String, String> guarded = ArchitecturyNetworkGuard.concurrentMap(original);
        assertEquals("initial", guarded.get("architectury:sync_ids"));
        assertEquals("initial", guarded.put("architectury:sync_ids", "replacement"));
        assertEquals("replacement", guarded.get("architectury:sync_ids"));
    }

    @Test
    void loginSnapshotsRemainSafeDuringParallelRegistration() throws Exception {
        Map<Integer, Integer> guarded = ArchitecturyNetworkGuard.concurrentMap(Map.of());
        int threadCount = 8;
        int registrationsPerThread = 1_000;
        var barrier = new CyclicBarrier(threadCount + 1);
        var executor = Executors.newFixedThreadPool(threadCount);

        try {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int thread = 0; thread < threadCount; thread++) {
                int offset = thread * registrationsPerThread;
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int registration = 0; registration < registrationsPerThread; registration++) {
                        int id = offset + registration;
                        guarded.put(id, id);
                    }
                    return null;
                }));
            }

            barrier.await();
            while (futures.stream().anyMatch(future -> !future.isDone())) {
                // This is the operation used by Architectury's sendSyncPacket method.
                new ArrayList<>(guarded.keySet());
            }
            for (var future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(threadCount * registrationsPerThread, guarded.size());
        assertEquals(threadCount * registrationsPerThread, new ArrayList<>(guarded.keySet()).size());
    }

    @Test
    void preservesSetBackedPlayerCapabilityState() {
        var serverIds = ArchitecturyNetworkGuard.concurrentSet(new HashSet<>(List.of("a", "b")));
        assertTrue(serverIds.containsAll(List.of("a", "b")));
        assertTrue(serverIds.add("c"));
        assertFalse(serverIds.add("c"));

        var original = HashMultimap.<String, String>create();
        original.put("player", "channel:a");
        var playerIds = ArchitecturyNetworkGuard.concurrentSetMultimap(original);
        assertTrue(playerIds.get("player") instanceof java.util.Set<?>);
        assertTrue(playerIds.get("player").contains("channel:a"));
        playerIds.get("player").clear();
        playerIds.get("player").add("channel:b");
        assertEquals(Set.of("channel:b"), playerIds.get("player"));
        playerIds.removeAll("player");
        assertFalse(playerIds.containsKey("player"));
    }
}

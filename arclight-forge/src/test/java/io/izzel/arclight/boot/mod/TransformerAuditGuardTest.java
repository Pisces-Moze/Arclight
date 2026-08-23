package io.izzel.arclight.boot.mod;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformerAuditGuardTest {

    @Test
    void installsIntoTheModLauncherAuditImplementation() throws Exception {
        var auditClass = Class.forName("cpw.mods.modlauncher.TransformerAuditTrail");
        var auditTrail = auditClass.getConstructor().newInstance();

        TransformerAuditGuard.install(auditTrail);

        var addReason = auditClass.getMethod("addReason", String.class, String.class);
        int threadCount = 8;
        int writesPerThread = 250;
        var barrier = new CyclicBarrier(threadCount);
        var executor = Executors.newFixedThreadPool(threadCount);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int write = 0; write < writesPerThread; write++) {
                        addReason.invoke(auditTrail, "org.luaj.SharedClass", "classloading");
                    }
                    return null;
                }));
            }
            for (var future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        var getActivity = auditClass.getMethod("getActivityFor", String.class);
        var activity = (List<?>) getActivity.invoke(auditTrail, "org.luaj.SharedClass");
        assertEquals(threadCount * writesPerThread, activity.size());
    }

    @Test
    void preservesExistingEntriesAndIterationOrder() {
        var map = new TransformerAuditGuard.ConcurrentAuditMap<Integer>(Map.of(
            "example.Class", new ArrayList<>(List.of(1, 2, 3))
        ));

        var entries = map.computeIfAbsent("example.Class", ignored -> new ArrayList<>());
        assertEquals(List.of(1, 2, 3), entries);
    }

    @Test
    void safelyRecordsConcurrentTransformations() throws Exception {
        var map = new TransformerAuditGuard.ConcurrentAuditMap<Integer>(Map.of());
        var entries = map.computeIfAbsent("org.luaj.SharedClass", ignored -> new ArrayList<>());
        int threadCount = 8;
        int writesPerThread = 500;
        var barrier = new CyclicBarrier(threadCount);
        var executor = Executors.newFixedThreadPool(threadCount);

        try {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int thread = 0; thread < threadCount; thread++) {
                int value = thread;
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int write = 0; write < writesPerThread; write++) {
                        entries.add(value);
                    }
                    return null;
                }));
            }
            for (var future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(threadCount * writesPerThread, entries.size());

        // Iterators retain a stable view if another transformer appends while diagnostics read it.
        var snapshot = entries.iterator();
        entries.add(-1);
        int snapshotSize = 0;
        while (snapshot.hasNext()) {
            snapshot.next();
            snapshotSize++;
        }
        assertEquals(threadCount * writesPerThread, snapshotSize);
    }
}

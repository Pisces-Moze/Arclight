package io.izzel.arclight.common.mod.server.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArclightEntitySchedulerTest {

    @Test
    void doesNotScheduleAnAlreadyRetiredEntity() {
        var valid = new AtomicBoolean(false);
        var scheduler = new SchedulerHarness();
        var subject = new ArclightEntityScheduler(entity(valid), scheduler.proxy());

        assertNull(subject.run(plugin(), ignored -> { }, null));
        assertFalse(subject.execute(plugin(), () -> { }, null, 1L));
        assertEquals(0, scheduler.submissions);
    }

    @Test
    void executesOnTheMainSchedulerAndPreservesDelay() {
        var valid = new AtomicBoolean(true);
        var scheduler = new SchedulerHarness();
        var subject = new ArclightEntityScheduler(entity(valid), scheduler.proxy());
        var calls = new AtomicInteger();

        var task = subject.runDelayed(plugin(), ignored -> calls.incrementAndGet(), null, 3L);
        assertNotNull(task);
        assertEquals(3L, scheduler.delay);
        assertFalse(scheduler.repeating);

        scheduler.submitted.run();
        assertEquals(1, calls.get());
        assertEquals(ScheduledTask.ExecutionState.FINISHED, task.getExecutionState());
    }

    @Test
    void retiresARepeatingTaskExactlyOnceWhenTheEntityIsRemoved() {
        var valid = new AtomicBoolean(true);
        var scheduler = new SchedulerHarness();
        var subject = new ArclightEntityScheduler(entity(valid), scheduler.proxy());
        var calls = new AtomicInteger();
        var retirements = new AtomicInteger();

        var task = subject.runAtFixedRate(plugin(), ignored -> calls.incrementAndGet(),
            retirements::incrementAndGet, 1L, 2L);
        assertNotNull(task);
        assertTrue(scheduler.repeating);

        scheduler.submitted.run();
        assertEquals(1, calls.get());
        valid.set(false);
        scheduler.submitted.run();
        scheduler.submitted.run();

        assertEquals(1, calls.get());
        assertEquals(1, retirements.get());
        assertTrue(scheduler.delegate.cancelled);
        assertEquals(ScheduledTask.ExecutionState.CANCELLED, task.getExecutionState());
    }

    @Test
    void executeNormalizesSubTickDelayToOneTick() {
        var valid = new AtomicBoolean(true);
        var scheduler = new SchedulerHarness();
        var subject = new ArclightEntityScheduler(entity(valid), scheduler.proxy());

        assertTrue(subject.execute(plugin(), () -> { }, null, 0L));
        assertEquals(1L, scheduler.delay);
    }

    private static Entity entity(AtomicBoolean valid) {
        return proxy(Entity.class, (proxy, method, args) -> switch (method.getName()) {
            case "isValid" -> valid.get();
            case "toString" -> "test-entity";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Plugin plugin() {
        return proxy(Plugin.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "test-plugin";
            case "isEnabled" -> true;
            case "toString" -> "test-plugin";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class SchedulerHarness implements InvocationHandler {

        private final TestBukkitTask delegate = new TestBukkitTask();
        private Runnable submitted;
        private long delay;
        private boolean repeating;
        private int submissions;

        BukkitScheduler proxy() {
            return ArclightEntitySchedulerTest.proxy(BukkitScheduler.class, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("runTaskLater") || method.getName().equals("runTaskTimer")) {
                this.submissions++;
                this.submitted = (Runnable) args[1];
                this.delay = (long) args[2];
                this.repeating = method.getName().equals("runTaskTimer");
                return this.delegate;
            }
            if (method.getName().equals("toString")) {
                return "test-scheduler";
            }
            throw new UnsupportedOperationException(method.toString());
        }
    }

    private static final class TestBukkitTask implements BukkitTask {

        private boolean cancelled;

        @Override public int getTaskId() { return 1; }
        @Override public Plugin getOwner() { return plugin(); }
        @Override public boolean isSync() { return true; }
        @Override public boolean isCancelled() { return this.cancelled; }
        @Override public void cancel() { this.cancelled = true; }
    }
}

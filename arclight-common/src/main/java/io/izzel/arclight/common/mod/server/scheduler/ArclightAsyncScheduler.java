package io.izzel.arclight.common.mod.server.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ArclightAsyncScheduler implements AsyncScheduler {

    private final BukkitScheduler scheduler;

    public ArclightAsyncScheduler(BukkitScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public ScheduledTask runNow(Plugin plugin, Consumer<ScheduledTask> task) {
        var handle = new ArclightScheduledTask(plugin, task, false);
        handle.bind(this.scheduler.runTaskAsynchronously(plugin, handle));
        return handle;
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task,
                                    long delay, TimeUnit unit) {
        long ticks = toTicks(delay, unit);
        var handle = new ArclightScheduledTask(plugin, task, false);
        handle.bind(this.scheduler.runTaskLaterAsynchronously(plugin, handle, ticks));
        return handle;
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task,
                                        long initialDelay, long period, TimeUnit unit) {
        long delayTicks = toTicks(initialDelay, unit);
        long periodTicks = toTicks(period, unit);
        var handle = new ArclightScheduledTask(plugin, task, true);
        handle.bind(this.scheduler.runTaskTimerAsynchronously(
            plugin, handle, delayTicks, periodTicks));
        return handle;
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        this.scheduler.cancelTasks(Objects.requireNonNull(plugin, "plugin"));
    }

    private static long toTicks(long time, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (time <= 0L) {
            throw new IllegalArgumentException("time must be positive");
        }
        long millis = unit.toMillis(time);
        return Math.max(1L, Math.addExact(millis, 49L) / 50L);
    }
}

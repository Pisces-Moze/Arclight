package io.izzel.arclight.common.mod.server.scheduler;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Entity scheduler for Arclight's single tick-thread ownership model.
 * Tasks stay on the normal server thread and check entity retirement immediately before execution.
 */
public final class ArclightEntityScheduler implements EntityScheduler {

    private final Entity entity;
    private final BukkitScheduler scheduler;

    public ArclightEntityScheduler(Entity entity, BukkitScheduler scheduler) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public boolean execute(Plugin plugin, Runnable run, Runnable retired, long delay) {
        Objects.requireNonNull(run, "run");
        return schedule(plugin, ignored -> run.run(), retired, Math.max(1L, delay), -1L) != null;
    }

    @Override
    public ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired) {
        return schedule(plugin, task, retired, 0L, -1L);
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task,
                                    Runnable retired, long delayTicks) {
        if (delayTicks <= 0L) {
            throw new IllegalArgumentException("delayTicks must be positive");
        }
        return schedule(plugin, task, retired, delayTicks, -1L);
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task,
                                        Runnable retired, long initialDelayTicks,
                                        long periodTicks) {
        if (initialDelayTicks <= 0L || periodTicks <= 0L) {
            throw new IllegalArgumentException("initial delay and period must be positive");
        }
        return schedule(plugin, task, retired, initialDelayTicks, periodTicks);
    }

    private ScheduledTask schedule(Plugin plugin, Consumer<ScheduledTask> action,
                                   Runnable retired, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(action, "action");
        if (!this.entity.isValid()) {
            return null;
        }

        var handle = new ArclightScheduledTask(plugin, action, periodTicks > 0L);
        Runnable dispatch = () -> {
            if (this.entity.isValid()) {
                handle.run();
                return;
            }

            var cancelled = handle.cancel();
            if (retired != null && (cancelled == ScheduledTask.CancelledState.CANCELLED_BY_CALLER
                || cancelled == ScheduledTask.CancelledState.NEXT_RUNS_CANCELLED)) {
                retired.run();
            }
        };
        var task = periodTicks > 0L
            ? this.scheduler.runTaskTimer(plugin, dispatch, delayTicks, periodTicks)
            : this.scheduler.runTaskLater(plugin, dispatch, delayTicks);
        handle.bind(task);
        return handle;
    }
}

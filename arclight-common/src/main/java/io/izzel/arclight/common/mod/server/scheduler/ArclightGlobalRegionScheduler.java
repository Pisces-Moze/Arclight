package io.izzel.arclight.common.mod.server.scheduler;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Objects;
import java.util.function.Consumer;

public final class ArclightGlobalRegionScheduler implements GlobalRegionScheduler {

    private final BukkitScheduler scheduler;

    public ArclightGlobalRegionScheduler(BukkitScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void execute(Plugin plugin, Runnable run) {
        this.scheduler.runTask(Objects.requireNonNull(plugin, "plugin"),
            Objects.requireNonNull(run, "run"));
    }

    @Override
    public ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task) {
        return schedule(plugin, task, 0L, -1L);
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        if (delayTicks <= 0L) {
            throw new IllegalArgumentException("delayTicks must be positive");
        }
        return schedule(plugin, task, delayTicks, -1L);
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task,
                                        long initialDelayTicks, long periodTicks) {
        if (initialDelayTicks <= 0L || periodTicks <= 0L) {
            throw new IllegalArgumentException("initial delay and period must be positive");
        }
        return schedule(plugin, task, initialDelayTicks, periodTicks);
    }

    private ScheduledTask schedule(Plugin plugin, Consumer<ScheduledTask> action,
                                   long delayTicks, long periodTicks) {
        var handle = new ArclightScheduledTask(plugin, action, periodTicks > 0L);
        var task = periodTicks > 0L
            ? this.scheduler.runTaskTimer(plugin, handle, delayTicks, periodTicks)
            : this.scheduler.runTaskLater(plugin, handle, delayTicks);
        handle.bind(task);
        return handle;
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        this.scheduler.cancelTasks(Objects.requireNonNull(plugin, "plugin"));
    }
}

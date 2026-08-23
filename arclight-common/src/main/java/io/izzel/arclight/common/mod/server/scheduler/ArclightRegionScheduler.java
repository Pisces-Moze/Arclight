package io.izzel.arclight.common.mod.server.scheduler;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Arclight is not region-threaded, so every region is owned by the normal server thread.
 */
public final class ArclightRegionScheduler implements RegionScheduler {

    private final ArclightGlobalRegionScheduler global;

    public ArclightRegionScheduler(ArclightGlobalRegionScheduler global) {
        this.global = Objects.requireNonNull(global, "global");
    }

    @Override
    public void execute(Plugin plugin, World world, int chunkX, int chunkZ, Runnable run) {
        Objects.requireNonNull(world, "world");
        this.global.execute(plugin, run);
    }

    @Override
    public ScheduledTask run(Plugin plugin, World world, int chunkX, int chunkZ,
                             Consumer<ScheduledTask> task) {
        Objects.requireNonNull(world, "world");
        return this.global.run(plugin, task);
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, World world, int chunkX, int chunkZ,
                                    Consumer<ScheduledTask> task, long delayTicks) {
        Objects.requireNonNull(world, "world");
        return this.global.runDelayed(plugin, task, delayTicks);
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, World world, int chunkX, int chunkZ,
                                        Consumer<ScheduledTask> task, long initialDelayTicks,
                                        long periodTicks) {
        Objects.requireNonNull(world, "world");
        return this.global.runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }
}

package io.izzel.arclight.common.bridge.bukkit;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;

/** Internal access used by Paper's static Bukkit scheduler facade. */
public interface PaperSchedulerBridge {

    GlobalRegionScheduler getGlobalRegionScheduler();

    RegionScheduler getRegionScheduler();

    AsyncScheduler getAsyncScheduler();
}

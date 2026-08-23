package io.izzel.arclight.common.mixin.bukkit;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Server;
import org.spongepowered.asm.mixin.Mixin;

/** Adds the targeted Paper scheduler surface to Arclight's Spigot Server interface. */
@Mixin(value = Server.class, remap = false)
public interface PaperServerApiMixin {

    GlobalRegionScheduler getGlobalRegionScheduler();

    RegionScheduler getRegionScheduler();

    AsyncScheduler getAsyncScheduler();
}

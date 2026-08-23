package io.izzel.arclight.common.mixin.bukkit;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.bukkit.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Adds Paper's entity scheduler contract to the Spigot Entity interface. */
@Mixin(value = Entity.class, remap = false)
public interface PaperEntityApiMixin {

    EntityScheduler getScheduler();
}

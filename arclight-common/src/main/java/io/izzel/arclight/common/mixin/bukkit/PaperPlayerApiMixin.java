package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Adds Paper's generalized respawn-location names to the Spigot Player interface. */
@Mixin(value = Player.class, remap = false)
public interface PaperPlayerApiMixin {

    Location getRespawnLocation();

    void setRespawnLocation(Location location);

    void setRespawnLocation(Location location, boolean force);
}

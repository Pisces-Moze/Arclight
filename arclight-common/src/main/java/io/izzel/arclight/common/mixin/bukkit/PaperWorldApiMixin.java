package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

/** Adds Paper's core asynchronous chunk-loading contract to the Spigot World interface. */
@Mixin(value = World.class, remap = false)
public interface PaperWorldApiMixin {

    CompletableFuture<Chunk> getChunkAtAsync(int chunkX, int chunkZ,
                                             boolean generate, boolean urgent);
}

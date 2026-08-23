package io.izzel.arclight.common.mixin.bukkit;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(value = CraftWorld.class, remap = false)
public abstract class CraftWorldMixin {

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public File getWorldFolder() {
        return ((ServerWorldBridge) this.world).bridge$getConvertable().getDimensionPath(this.world.dimension()).toFile();
    }

    /**
     * Paper-compatible asynchronous FULL chunk load backed by Minecraft's chunk future.
     * Calling ServerChunkCache#getChunkFuture on the server thread blocks by design, so the
     * initial request is submitted off-thread; Minecraft immediately marshals ticket and
     * holder work back to its main-thread executor and completes the future asynchronously.
     */
    public CompletableFuture<Chunk> getChunkAtAsync(int chunkX, int chunkZ,
                                                     boolean generate, boolean urgent) {
        Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> request =
            () -> this.world.getChunkSource().getChunkFuture(
                chunkX, chunkZ, ChunkStatus.FULL, generate);

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> loading;
        if (this.world.getServer().isSameThread()) {
            loading = CompletableFuture.supplyAsync(request).thenCompose(future -> future);
        } else {
            loading = request.get();
        }

        var result = new CompletableFuture<Chunk>();
        loading.whenComplete((either, failure) -> this.world.getServer().execute(() -> {
            if (failure != null) {
                result.completeExceptionally(failure);
                return;
            }
            ChunkAccess access = either.left().orElse(null);
            if (access instanceof LevelChunk levelChunk) {
                result.complete(new CraftChunk(levelChunk));
            } else {
                var loadingFailure = either.right().orElse(null);
                result.completeExceptionally(new IllegalStateException(
                    "Chunk " + chunkX + "," + chunkZ + " failed to reach FULL status: "
                        + loadingFailure));
            }
        }));
        return result;
    }

    @Redirect(method = "getHumidity(III)D", at = @At(value = "FIELD", remap = true, target = "Lnet/minecraft/world/level/biome/Biome;climateSettings:Lnet/minecraft/world/level/biome/Biome$ClimateSettings;"))
    private Biome.ClimateSettings arclight$useForgeSetting(Biome instance) {
        return instance.getModifiedClimateSettings();
    }
}

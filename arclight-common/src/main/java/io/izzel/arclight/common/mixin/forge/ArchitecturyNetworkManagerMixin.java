package io.izzel.arclight.common.mixin.forge;

import com.google.common.collect.Multimap;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.util.ArchitecturyNetworkGuard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;
import java.util.Set;

/**
 * Architectury 9.2.14 registers receivers while Forge constructs mods in parallel, but keeps its
 * global networking state in HashMap/HashSet instances. Replace those instances before the packet
 * listener captures C2S_TRANSFORMERS so registrations, packet lookup and login snapshots all see
 * the same concurrency-safe state.
 */
@Pseudo
@Mixin(targets = "dev.architectury.networking.forge.NetworkManagerImpl", remap = false)
@LoadIfMod(modid = {"architectury"}, condition = LoadIfMod.ModCondition.PRESENT)
public abstract class ArchitecturyNetworkManagerMixin {

    @Shadow @Final @Mutable private static Map<Object, Object> S2C;
    @Shadow @Final @Mutable private static Map<Object, Object> C2S;
    @Shadow @Final @Mutable private static Map<Object, Object> S2C_TRANSFORMERS;
    @Shadow @Final @Mutable private static Map<Object, Object> C2S_TRANSFORMERS;
    @Shadow @Final @Mutable private static Set<Object> serverReceivables;
    @Shadow @Final @Mutable private static Multimap<Object, Object> clientReceivables;

    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Ldev/architectury/networking/forge/NetworkManagerImpl;createPacketHandler(Ljava/lang/Class;Ljava/util/Map;)Ljava/util/function/Consumer;"
        ),
        index = 1,
        remap = false
    )
    private static Map<Object, Object> arclight$installConcurrentNetworkState(Map<Object, Object> capturedTransformers) {
        C2S = ArchitecturyNetworkGuard.concurrentMap(C2S);
        S2C = ArchitecturyNetworkGuard.concurrentMap(S2C);
        C2S_TRANSFORMERS = ArchitecturyNetworkGuard.concurrentMap(capturedTransformers);
        S2C_TRANSFORMERS = ArchitecturyNetworkGuard.concurrentMap(S2C_TRANSFORMERS);
        serverReceivables = ArchitecturyNetworkGuard.concurrentSet(serverReceivables);
        clientReceivables = ArchitecturyNetworkGuard.concurrentSetMultimap(clientReceivables);
        return C2S_TRANSFORMERS;
    }
}

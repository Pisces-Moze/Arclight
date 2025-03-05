package io.izzel.arclight.common.mixin.core.network;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@SuppressWarnings("target")
@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1500)
public abstract class ServerGamePacketListenerImplMixin_HighPriority {

    @Shadow private Vec3 awaitingPositionFromClient;

    // These fields are from our ServerGamePacketListenerImplMixin.
    @Shadow(remap = false) private double lastPosX;
    @Shadow(remap = false) private double lastPosY;
    @Shadow(remap = false) private double lastPosZ;
    @Shadow(remap = false) private float lastPitch;
    @Shadow(remap = false) private float lastYaw;


    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;awaitingTeleportTime:I"))
    private void arclight$storeLastPosition(double d, double e, double f, float yaw, float pitch, Set<RelativeMovement> set, CallbackInfo ci) {
        this.lastPosX = this.awaitingPositionFromClient.x;
        this.lastPosY = this.awaitingPositionFromClient.y;
        this.lastPosZ = this.awaitingPositionFromClient.z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }
}

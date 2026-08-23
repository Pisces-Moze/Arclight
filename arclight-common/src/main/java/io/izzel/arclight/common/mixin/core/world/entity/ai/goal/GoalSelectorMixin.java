package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Keeps goal iteration stable when a mod or plugin changes a selector while one of its goals
 * is ticking. Goal selectors are read on every entity tick and are changed comparatively rarely,
 * which makes a snapshot-on-write set both safe and cheap for this workload.
 */
@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {

    @Shadow @Final @Mutable private Set<WrappedGoal> availableGoals;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$useStableGoalSnapshots(CallbackInfo ci) {
        this.availableGoals = new CopyOnWriteArraySet<>(this.availableGoals);
    }
}

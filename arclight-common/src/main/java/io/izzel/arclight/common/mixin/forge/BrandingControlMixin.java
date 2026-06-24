package io.izzel.arclight.common.mixin.forge;

import net.minecraftforge.internal.BrandingControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BrandingControl.class, remap = false)
public class BrandingControlMixin {

    /**
     * @author Arclight Team
     */
    @Overwrite
    public static String getServerBranding() {
        return "MoLight2.4";
    }
}

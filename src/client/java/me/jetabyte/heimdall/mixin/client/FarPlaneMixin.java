package me.jetabyte.heimdall.mixin.client;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class FarPlaneMixin {
    @Inject(method = "getFarPlaneDistance", at = @At("RETURN"), cancellable = true)
    private void increaseFarPlane(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(4096.0f);
    }
}

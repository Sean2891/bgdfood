package com.bgd.food.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HungerHudMixin {
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void bgd$hideVanillaFood(DrawContext context, PlayerEntity player,
                                     int top, int right, CallbackInfo ci) {
        ci.cancel();   // 干掉原版 10 图标
    }
}

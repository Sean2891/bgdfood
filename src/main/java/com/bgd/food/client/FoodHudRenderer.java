package com.bgd.food.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class FoodHudRenderer implements ClientModInitializer {

    private static final int SHANKS = 3;
    private static final Identifier FULL  = Identifier.ofVanilla("hud/food_full");
    private static final Identifier HALF  = Identifier.ofVanilla("hud/food_half");
    private static final Identifier EMPTY = Identifier.ofVanilla("hud/food_empty");

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((DrawContext ctx, var tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            if (mc.interactionManager == null
                || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) return;

            int food = Math.min(mc.player.getHungerManager().getFoodLevel(), SHANKS * 2);
            int right = ctx.getScaledWindowWidth() / 2 + 91;
            int y = ctx.getScaledWindowHeight() - 39;

            for (int i = 0; i < SHANKS; i++) {
                int x = right - i * 8 - 9;
                int pts = food - i * 2;
                Identifier tex = pts >= 2 ? FULL : (pts == 1 ? HALF : EMPTY);
                ctx.drawGuiTexture(tex, x, y, 9, 9);
            }
        });
    }
}

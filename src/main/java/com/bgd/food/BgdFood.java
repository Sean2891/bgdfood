package com.bgd.food;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class BgdFood implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(CommandManager.literal("bgdfood")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("hunger", IntegerArgumentType.integer(0, 20))
                        .executes(ctx -> apply(ctx, -1f))
                        .then(CommandManager.argument("saturation", FloatArgumentType.floatArg(0f, 20f))
                            .executes(ctx -> apply(ctx, FloatArgumentType.getFloat(ctx, "saturation"))))))));
    }

    private static int apply(CommandContext<ServerCommandSource> ctx, float saturation)
            throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
        int hunger = IntegerArgumentType.getInteger(ctx, "hunger");

        for (ServerPlayerEntity p : targets) {
            p.getHungerManager().setFoodLevel(hunger);
            if (saturation >= 0f) p.getHungerManager().setSaturationLevel(saturation);

            // 立刻同步给客户端，否则 HUD 要等下一次自然刷新
            p.networkHandler.sendPacket(new HealthUpdateS2CPacket(
                p.getHealth(),
                p.getHungerManager().getFoodLevel(),
                p.getHungerManager().getSaturationLevel()));
        }

        ctx.getSource().sendFeedback(() -> Text.literal(
            "已设置 " + targets.size() + " 名玩家：饥饿 " + hunger
            + (saturation >= 0f ? "，饱和 " + saturation : "")), false);
        return targets.size();
    }
}

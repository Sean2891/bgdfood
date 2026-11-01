package com.bgd.food;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BgdFood implements ModInitializer {

    /** uuid -> { 饥饿上限, 饱和上限 × 100 } */
    public static final Map<UUID, int[]> CAPS = new HashMap<>();

    @Override
    public void onInitialize() {

        /* ---------- 1. /bgdfood <targets> <hunger> [saturation] ---------- */
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(CommandManager.literal("bgdfood")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("hunger", IntegerArgumentType.integer(0, 20))
                        .executes(ctx -> applySet(ctx, -1f))
                        .then(CommandManager.argument("saturation", FloatArgumentType.floatArg(0f, 20f))
                            .executes(ctx -> applySet(ctx, FloatArgumentType.getFloat(ctx, "saturation"))))))));

        /* ---------- 2. /bgdfoodcap <targets> <hunger> <saturation> ---------- */
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(CommandManager.literal("bgdfoodcap")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(CommandManager.argument("hunger", IntegerArgumentType.integer(0, 20))
                        .then(CommandManager.argument("saturation", FloatArgumentType.floatArg(0f, 20f))
                            .executes(BgdFood::applyCap))))));

        /* ---------- 3. 每 tick 强制执行上限 ---------- */
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (CAPS.containsKey(p.getUuid())) clamp(p);
            }
        });

        /* ---------- 4. 玩家退出时清理记录，避免内存泄漏 ---------- */
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            CAPS.remove(handler.player.getUuid()));
    }

    /* ================= /bgdfood：直接设置 ================= */
    private static int applySet(CommandContext<ServerCommandSource> ctx, float saturation)
            throws CommandSyntaxException {

        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
        int hunger = IntegerArgumentType.getInteger(ctx, "hunger");

        for (ServerPlayerEntity p : targets) {
            p.getHungerManager().setFoodLevel(hunger);
            if (saturation >= 0f) {
                p.getHungerManager().setSaturationLevel(saturation);
            }
            sync(p);
        }

        ctx.getSource().sendFeedback(() -> Text.literal(
            "已设置 " + targets.size() + " 名玩家：饥饿 " + hunger
            + (saturation >= 0f ? "，饱和 " + saturation : "")), false);
        return targets.size();
    }

    /* ================= /bgdfoodcap：设置上限 ================= */
    private static int applyCap(CommandContext<ServerCommandSource> ctx)
            throws CommandSyntaxException {

        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
        int hunger = IntegerArgumentType.getInteger(ctx, "hunger");
        float saturation = FloatArgumentType.getFloat(ctx, "saturation");

        for (ServerPlayerEntity p : targets) {
            CAPS.put(p.getUuid(), new int[]{ hunger, Math.round(saturation * 100f) });
            clamp(p);
        }

        ctx.getSource().sendFeedback(() -> Text.literal(
            "已设置上限：" + targets.size() + " 名玩家，饥饿 " + hunger
            + " / 饱和 " + saturation), false);
        return targets.size();
    }

    /* ================= 削顶逻辑 ================= */
    public static void clamp(ServerPlayerEntity p) {
        int[] cap = CAPS.get(p.getUuid());
        if (cap == null) return;

        HungerManager hm = p.getHungerManager();
        boolean changed = false;

        if (hm.getFoodLevel() > cap[0]) {
            hm.setFoodLevel(cap[0]);
            changed = true;
        }

        // 饱和度不能高于饥饿值，两个上限取小
        float satCap = Math.min(cap[1] / 100f, hm.getFoodLevel());
        if (hm.getSaturationLevel() > satCap) {
            hm.setSaturationLevel(satCap);
            changed = true;
        }

        if (changed) sync(p);
    }

    /* ================= 同步给客户端 ================= */
    private static void sync(ServerPlayerEntity p) {
        p.networkHandler.sendPacket(new HealthUpdateS2CPacket(
            p.getHealth(),
            p.getHungerManager().getFoodLevel(),
            p.getHungerManager().getSaturationLevel()));
    }
}

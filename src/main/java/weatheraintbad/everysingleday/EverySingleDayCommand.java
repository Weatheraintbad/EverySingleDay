package weatheraintbad.everysingleday;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

import static weatheraintbad.everysingleday.EverySingleDay.*;

public final class EverySingleDayCommand {

    /* 仅保留 24 条效果（14 正 + 10 负）*/
    private static final List<String> VALID_EFFECTS = List.of(
            // 14 正
            "farming", "combat", "speed", "health", "luck",
            "phoenix", "magnet", "night_vision", "fire_immunity", "water_breathing",
            "vampire", "treasure_hunter", "shadow_step", "elemental_shield",
            // 10 负
            "water", "hunger", "weakness", "slowness", "mining_fatigue",
            "unluck", "explosive_death", "sun_allergy", "noise_maker", "storm_maker"
    );

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("esd")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("effect", StringArgumentType.string())
                                        .suggests((c, b) -> {
                                            VALID_EFFECTS.forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> give(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "effect"))))))
                .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> clear(
                                        ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "player")))))
                .then(CommandManager.literal("reload")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> reload(
                                        ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "player"))))));
    }

    private static int list(ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal("§6=== 可用效果列表（24 条）==="), false);
        VALID_EFFECTS.forEach(id ->
                src.sendFeedback(() -> Text.literal("§b" + id), false));
        return 1;
    }

    private static int give(ServerCommandSource src, ServerPlayerEntity target, String id) {
        if (!VALID_EFFECTS.contains(id)) {
            src.sendError(Text.literal("§c未知效果: " + id));
            return 0;
        }
        DailyEffect eff = find(id);
        if (eff == null) {
            src.sendError(Text.literal("§c效果未找到: " + id));
            return 0;
        }
        PlayerDailyEffects data = playerEffects.computeIfAbsent(target.getUuid(), u -> new PlayerDailyEffects());
        if (POSITIVE_EFFECTS.contains(eff)) data.positiveEffect = eff;
        else data.negativeEffect = eff;
        EverySingleDay.applyEffectsStatic(target, data);
        src.sendFeedback(() -> Text.literal("§a已给 §f" + target.getName().getString() + " §a附加效果 §e" + id), true);
        return 1;
    }

    private static int reload(ServerCommandSource src, ServerPlayerEntity target) {
        long day = target.getServer().getOverworld().getTimeOfDay() / 24000L;
        EverySingleDay.generateNewDailyEffectsStatic(target, day);
        src.sendFeedback(() -> Text.literal("§a已重载 §f" + target.getName().getString() + " §a的每日效果"), true);
        return 1;
    }

    /* 唯一一份 clear：合并事件层清除 */
    private static int clear(ServerCommandSource src, ServerPlayerEntity target) {
        // 1. 清除原版状态效果
        EverySingleDay.clearOldEffectsStatic(target);
        // 2. 清除事件层效果（拾取、掉落、闪电、动物 Goal）
        EffectEventListener.clearPlayerEffects(target);
        // 3. 广播
        src.sendFeedback(() -> Text.literal("§a已清除 §f" + target.getName().getString() + " §a的全部效果（含事件层）"), true);
        return 1;
    }

    private static DailyEffect find(String id) {
        return java.util.stream.Stream.of(POSITIVE_EFFECTS, NEGATIVE_EFFECTS)
                .flatMap(java.util.List::stream)
                .filter(e -> e.id.equals(id))
                .findFirst()
                .orElse(null);
    }
}
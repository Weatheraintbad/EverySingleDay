package weatheraintbad.everysingleday;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EffectsManager {

    public static void applyPositiveEffect(ServerPlayerEntity player, EverySingleDay.DailyEffect effect) {
        switch (effect.id) {
            // 基础效果
            case "mining":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 24000, 2));
                break;
            case "farming":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 24000, 1));
                break;
            case "combat":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 24000, 1));
                break;
            case "speed":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 24000, 2));
                break;
            case "health":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 24000, 2));
                break;
            case "luck":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 24000, 2));
                break;

            // 新增效果
            case "phoenix":
                player.sendMessage(Text.literal("🔥 凤凰之力：死亡时将重生并爆炸！")
                        .formatted(Formatting.GOLD, Formatting.BOLD));
                break;

            case "magnet":
                player.sendMessage(Text.literal("🧲 磁铁激活：自动吸引附近物品！")
                        .formatted(Formatting.BLUE));
                break;

            case "night_vision":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 24000, 0));
                break;

            case "fire_immunity":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 24000, 0));
                break;

            case "water_breathing":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 24000, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 24000, 1));
                break;

            /* 二段跳已删除 */
            case "vampire":
                player.sendMessage(Text.literal("🧛 吸血鬼之触：攻击敌人时回复生命！")
                        .formatted(Formatting.DARK_RED));
                break;

            case "thorns":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24000, 0));
                break;

            case "treasure_hunter":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 24000, 3));
                player.sendMessage(Text.literal("🏴‍☠️ 寻宝者直觉：发现隐藏的宝藏！")
                        .formatted(Formatting.GOLD));
                break;

            case "time_master":
                player.sendMessage(Text.literal("⏰ 时间掌控：时间流逝速度减半！")
                        .formatted(Formatting.AQUA));
                break;

            case "super_craft":
                player.sendMessage(Text.literal("🔨 巧匠之手：制作时有几率双倍产出！")
                        .formatted(Formatting.YELLOW));
                break;

            case "animal_whisperer":
                player.sendMessage(Text.literal("🐺 动物之友：动物会主动保护你！")
                        .formatted(Formatting.GREEN));
                break;

            case "shadow_step":
                // 不再在这里添加效果，改为实时处理
                player.sendMessage(Text.literal("🌑 暗影步：潜行时获得隐身和速度！")
                        .formatted(Formatting.DARK_GRAY));
                break;

            case "elemental_shield":
                player.sendMessage(Text.literal("🛡️ 元素护盾：周期性获得随机元素保护！")
                        .formatted(Formatting.BLUE));
                break;
        }
    }

    public static void applyNegativeEffect(ServerPlayerEntity player, EverySingleDay.DailyEffect effect) {
        switch (effect.id) {
            // 基础效果
            case "water":
                // 水下减速效果通过事件监听实现
                break;
            case "hunger":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 24000, 2));
                break;
            case "weakness":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 24000, 1));
                break;
            case "slowness":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 24000, 2));
                break;
            case "mining_fatigue":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 24000, 2));
                break;
            case "unluck":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.UNLUCK, 24000, 2));
                break;

            // 新增负面效果
            case "explosive_death":
                player.sendMessage(Text.literal("💀 爆炸诅咒：死亡时将产生爆炸！")
                        .formatted(Formatting.RED, Formatting.BOLD));
                break;

            case "item_magnet":
                player.sendMessage(Text.literal("🚫 物品排斥：无法捡起地面物品！")
                        .formatted(Formatting.GRAY));
                break;

            case "sun_allergy":
                player.sendMessage(Text.literal("☀️ 阳光过敏：白天在阳光下会受伤！")
                        .formatted(Formatting.YELLOW));
                break;

            case "noise_maker":
                player.sendMessage(Text.literal("🔊 噪音制造者：持续发出声音吸引怪物！")
                        .formatted(Formatting.DARK_RED));
                break;

            case "clumsy":
                player.sendMessage(Text.literal("🤲 笨拙之手：有概率掉落手持物品！")
                        .formatted(Formatting.GOLD));
                break;

            case "sleepwalker":
                // 不再在这里处理，改为实时处理
                player.sendMessage(Text.literal("😴 梦游症：睡觉时可能随机传送！")
                        .formatted(Formatting.DARK_PURPLE));
                break;

            case "broken_armor":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 24000, 0));
                break;

            case "confusion":
                // 不再在这里处理，改为实时处理
                player.sendMessage(Text.literal("🌀 方向混乱：你的方向感错乱了！")
                        .formatted(Formatting.LIGHT_PURPLE));
                break;

            case "money_curse":
                player.sendMessage(Text.literal("💸 破财诅咒：死亡时掉落双倍经验！")
                        .formatted(Formatting.DARK_GREEN));
                break;

            case "storm_maker":
                player.sendMessage(Text.literal("⛈️ 风暴召唤：你持续吸引闪电！")
                        .formatted(Formatting.DARK_BLUE));
                break;

            case "fragile":
                player.sendMessage(Text.literal("🩸 脆弱之躯：受到的伤害+50%！")
                        .formatted(Formatting.RED));
                break;

            case "hated_by_animals":
                player.sendMessage(Text.literal("🐄 动物公敌：动物会主动攻击你！")
                        .formatted(Formatting.DARK_RED));
                break;

            case "gravity_well":
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 24000, -1));
                break;
        }
    }
}
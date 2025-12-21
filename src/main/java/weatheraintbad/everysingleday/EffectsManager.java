package weatheraintbad.everysingleday;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class EffectsManager {

    public static void applyPositiveEffect(ServerPlayerEntity player, EverySingleDay.DailyEffect effect) {
        switch (effect.id) {
            // 基础效果
            case "farming" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 24000, 1));
            case "combat" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 24000, 1));
            case "speed" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 24000, 2));
            case "health" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 24000, 2));
            case "luck" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 24000, 2));
            // 新增效果（仅加状态，不聊天）
            case "phoenix" -> {} // 死亡事件由 EverySingleDay 统一处理
            case "magnet" -> {} // 每 tick 统一处理
            case "night_vision" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 24000, 0));
            case "fire_immunity" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 24000, 0));
            case "water_breathing" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 24000, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 24000, 1));
            }
            case "vampire" -> {} // 攻击事件统一处理
            case "thorns" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24000, 0));
            case "treasure_hunter" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 24000, 3));
            case "time_master" -> {} // 时间流逝由服务器逻辑处理
            case "shadow_step" -> {} // 潜行事件统一处理
            case "elemental_shield" -> {} // 周期性护盾统一处理
        }
    }

    public static void applyNegativeEffect(ServerPlayerEntity player, EverySingleDay.DailyEffect effect) {
        switch (effect.id) {
            case "water" -> {} // 水下减速由事件处理
            case "hunger" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 24000, 2));
            case "weakness" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 24000, 1));
            case "slowness" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 24000, 2));
            case "mining_fatigue" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 24000, 2));
            case "unluck" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.UNLUCK, 24000, 2));
            case "explosive_death" -> {} // 死亡事件统一处理
            case "sun_allergy" -> {} // 白天受伤由事件处理
            case "noise_maker" -> {} // 噪音由事件处理
            case "confusion" -> {} // 视角旋转由事件处理
            case "storm_maker" -> {} // 引雷由事件处理
        }
    }
}
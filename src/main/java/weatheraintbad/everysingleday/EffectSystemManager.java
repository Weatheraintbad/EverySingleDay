package weatheraintbad.everysingleday;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.GameRules;

import java.util.*;

public class EffectSystemManager {

    private static final Map<UUID, PlayerEffectState> playerStates = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static class PlayerEffectState {
        public boolean isShadowStepping = false;
        public boolean isSleepwalker = false;
        public boolean isConfused = false;
        public BlockPos lastSleepPos = null;
        public int confusionTimer = 0;
        public int nextConfusionTick = 0;
        public int nextSleepwalkTick = 0;
    }

    public static PlayerEffectState getPlayerState(ServerPlayerEntity player) {
        return playerStates.computeIfAbsent(player.getUuid(), uuid -> new PlayerEffectState());
    }

    // 暗影步效果 - 在玩家潜行时调用
    public static void handleShadowStep(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);

        if (!state.isShadowStepping) {
            state.isShadowStepping = true;
            // 添加隐身效果
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 100, 0, false, false));
            // 添加速度效果
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, false, false));
            player.sendMessage(Text.literal("🌑 你已融入暗影中...").formatted(Formatting.DARK_GRAY), true);
        }
    }

    // 停止暗影步效果 - 在玩家停止潜行时调用
    public static void stopShadowStep(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);

        if (state.isShadowStepping) {
            state.isShadowStepping = false;
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
            player.removeStatusEffect(StatusEffects.SPEED);
            player.sendMessage(Text.literal("👤 你从暗影中现身").formatted(Formatting.GRAY), true);
        }
    }

    // 处理梦游效果
    public static void handleSleepwalker(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);

        if (!player.isSleeping()) {
            state.lastSleepPos = null;
            return;
        }

        // 检查是否应该触发梦游
        int currentTick = player.getServer().getTicks();
        if (currentTick >= state.nextSleepwalkTick) {
            state.nextSleepwalkTick = currentTick + 1000 + RANDOM.nextInt(2000); // 50-100秒后再次检查

            if (RANDOM.nextInt(100) < 15) { // 15% 概率触发梦游
                triggerSleepwalk(player, state);
            }
        }
    }

    private static void triggerSleepwalk(ServerPlayerEntity player, PlayerEffectState state) {
        BlockPos currentPos = player.getBlockPos();

        // 寻找附近的随机位置
        BlockPos newPos = currentPos.add(
                RANDOM.nextInt(64) - 32,  // 更大的范围
                0,
                RANDOM.nextInt(64) - 32
        );

        // 找到安全的地表位置
        ServerWorld world = player.getServerWorld();
        newPos = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, newPos);

        // 确保位置安全
        if (world.getBlockState(newPos).isAir() && world.getBlockState(newPos.down()).isSolid()) {
            player.wakeUp();
            player.requestTeleport(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);

            // 添加一些效果
            world.playSound(null, newPos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);

            player.sendMessage(Text.literal("😴 梦游了！你醒来发现在一个陌生的地方")
                    .formatted(Formatting.DARK_PURPLE));

            // 添加短暂的迷茫效果
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
        }
    }

    // 处理混乱效果
    public static void handleConfusion(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);
        int currentTick = player.getServer().getTicks();

        // 初始化混乱计时器
        if (state.nextConfusionTick == 0) {
            state.nextConfusionTick = currentTick + 40 + RANDOM.nextInt(120); // 2-8秒后开始
        }

        if (currentTick >= state.nextConfusionTick) {
            state.nextConfusionTick = currentTick + 20 + RANDOM.nextInt(100); // 1-5秒后再次触发

            // 应用视角旋转效果
            applyConfusionEffect(player);
        }
    }

    private static void applyConfusionEffect(ServerPlayerEntity player) {
        // 随机旋转玩家的视角
        float randomYaw = RANDOM.nextFloat() * 360.0f;
        float randomPitch = (RANDOM.nextFloat() - 0.5f) * 90.0f;

        // 设置玩家视角
        player.setYaw(randomYaw);
        player.setPitch(randomPitch);

        // 发送混乱消息
        player.sendMessage(Text.literal("🌀 方向感突然错乱！").formatted(Formatting.LIGHT_PURPLE), true);

        // 添加轻微的恶心效果（通过屏幕抖动模拟）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));

        // 随机传送一小段距离（模拟空间错乱）
        if (RANDOM.nextInt(100) < 30) { // 30% 概率小传送
            Vec3d currentPos = player.getPos();
            Vec3d randomOffset = new Vec3d(
                    (RANDOM.nextDouble() - 0.5) * 6.0,  // ±3格
                    0,
                    (RANDOM.nextDouble() - 0.5) * 6.0   // ±3格
            );

            Vec3d newPos = currentPos.add(randomOffset);
            BlockPos blockPos = new BlockPos((int)newPos.x, (int)currentPos.y, (int)newPos.z);

            // 确保目标位置安全
            if (player.getWorld().getBlockState(blockPos).isAir() &&
                    player.getWorld().getBlockState(blockPos.down()).isSolid()) {

                player.requestTeleport(newPos.x, currentPos.y, newPos.z);
                player.getWorld().playSound(null, blockPos, SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                        SoundCategory.PLAYERS, 0.5f, 2.0f);
            }
        }
    }

    // 检查玩家是否有特定效果
    public static boolean hasEffect(ServerPlayerEntity player, String effectId) {
        // 这里需要从主类获取玩家的当前效果
        // 简化实现：通过检查玩家状态
        PlayerEffectState state = getPlayerState(player);

        switch (effectId) {
            case "shadow_step":
                return state.isShadowStepping;
            case "sleepwalker":
                return state.isSleepwalker;
            case "confusion":
                return state.isConfused;
            default:
                return false;
        }
    }

    // 重置玩家的效果状态
    public static void resetPlayerEffects(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);

        if (state.isShadowStepping) {
            stopShadowStep(player);
        }

        state.isSleepwalker = false;
        state.isConfused = false;
        state.confusionTimer = 0;
        state.nextConfusionTick = 0;
        state.nextSleepwalkTick = 0;
    }
}
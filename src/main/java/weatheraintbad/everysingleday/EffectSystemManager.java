package weatheraintbad.everysingleday;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

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

    /* ===== 暗影步 ===== */
    public static void handleShadowStep(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);
        if (!state.isShadowStepping) {
            state.isShadowStepping = true;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 100, 0, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, false, false));
            player.sendMessage(Text.translatable("everysingleday.shadow_step.on"), true);
        }
    }

    public static void stopShadowStep(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);
        if (state.isShadowStepping) {
            state.isShadowStepping = false;
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
            player.removeStatusEffect(StatusEffects.SPEED);
            player.sendMessage(Text.translatable("everysingleday.shadow_step.off"), true);
        }
    }

    /* ===== 梦游 ===== */
    public static void handleSleepwalker(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);
        if (!player.isSleeping()) {
            state.lastSleepPos = null;
            return;
        }
        int currentTick = player.getServer().getTicks();
        if (currentTick >= state.nextSleepwalkTick) {
            state.nextSleepwalkTick = currentTick + 1000 + RANDOM.nextInt(2000);
            if (RANDOM.nextInt(100) < 15) triggerSleepwalk(player, state);
        }
    }

    private static void triggerSleepwalk(ServerPlayerEntity player, PlayerEffectState state) {
        BlockPos currentPos = player.getBlockPos();
        BlockPos newPos = currentPos.add(RANDOM.nextInt(64) - 32, 0, RANDOM.nextInt(64) - 32);
        ServerWorld world = player.getServerWorld();
        newPos = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, newPos);
        if (world.getBlockState(newPos).isAir() && world.getBlockState(newPos.down()).isSolid()) {
            player.wakeUp();
            player.requestTeleport(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);
            world.playSound(null, newPos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            player.sendMessage(Text.translatable("everysingleday.sleepwalker.teleport"), false);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
        }
    }

    /* ===== 混乱 ===== */
    public static void handleConfusion(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);
        int currentTick = player.getServer().getTicks();
        if (state.nextConfusionTick == 0) state.nextConfusionTick = currentTick + 40 + RANDOM.nextInt(120);
        if (currentTick >= state.nextConfusionTick) {
            state.nextConfusionTick = currentTick + 20 + RANDOM.nextInt(100);
            applyConfusionEffect(player);
        }
    }

    private static void applyConfusionEffect(ServerPlayerEntity player) {
        float randomYaw = RANDOM.nextFloat() * 360.0f;
        float randomPitch = (RANDOM.nextFloat() - 0.5f) * 90.0f;
        player.setYaw(randomYaw);
        player.setPitch(randomPitch);
        player.sendMessage(Text.translatable("everysingleday.confusion.spin"), true);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
        if (RANDOM.nextInt(100) < 30) {
            Vec3d currentPos = player.getPos();
            Vec3d randomOffset = new Vec3d((RANDOM.nextDouble() - 0.5) * 6.0, 0, (RANDOM.nextDouble() - 0.5) * 6.0);
            Vec3d newPos = currentPos.add(randomOffset);
            BlockPos blockPos = new BlockPos((int)newPos.x, (int)currentPos.y, (int)newPos.z);
            if (player.getWorld().getBlockState(blockPos).isAir() && player.getWorld().getBlockState(blockPos.down()).isSolid()) {
                player.requestTeleport(newPos.x, currentPos.y, newPos.z);
                player.getWorld().playSound(null, blockPos, SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                        SoundCategory.PLAYERS, 0.5f, 2.0f);
            }
        }
    }

    /* ===== 工具方法 ===== */
    public static boolean hasEffect(ServerPlayerEntity player, String effectId) {
        PlayerEffectState state = getPlayerState(player);
        switch (effectId) {
            case "shadow_step": return state.isShadowStepping;
            case "sleepwalker": return state.isSleepwalker;
            case "confusion":   return state.isConfused;
            default:            return false;
        }
    }

    public static void resetPlayerEffects(ServerPlayerEntity player) {
        PlayerEffectState state = getPlayerState(player);
        if (state.isShadowStepping) stopShadowStep(player);
        state.isSleepwalker = false;
        state.isConfused = false;
        state.confusionTimer = 0;
        state.nextConfusionTick = 0;
        state.nextSleepwalkTick = 0;
    }
}
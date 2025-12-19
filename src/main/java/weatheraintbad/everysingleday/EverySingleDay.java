package weatheraintbad.everysingleday;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ItemEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EverySingleDay implements ModInitializer {
    public static final String MOD_ID = "everysingleday";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /* ---------- 效果列表（全部使用语言 key） ---------- */
    public static final List<DailyEffect> POSITIVE_EFFECTS = Arrays.asList(
            new DailyEffect("mining", "everysingleday.suitable.mining", "everysingleday.desc.mining", 0.5f),
            new DailyEffect("farming", "everysingleday.suitable.farming", "everysingleday.desc.farming", 0.5f),
            new DailyEffect("combat", "everysingleday.suitable.combat", "everysingleday.desc.combat", 0.2f),
            new DailyEffect("speed", "everysingleday.suitable.speed", "everysingleday.desc.speed", 0.3f),
            new DailyEffect("health", "everysingleday.suitable.health", "everysingleday.desc.health", 1.0f),
            new DailyEffect("luck", "everysingleday.suitable.luck", "everysingleday.desc.luck", 0.0f),
            new DailyEffect("phoenix", "everysingleday.suitable.phoenix", "everysingleday.desc.phoenix", 0.0f),
            new DailyEffect("magnet", "everysingleday.suitable.magnet", "everysingleday.desc.magnet", 8.0f),
            new DailyEffect("night_vision", "everysingleday.suitable.night_vision", "everysingleday.desc.night_vision", 0.0f),
            new DailyEffect("fire_immunity", "everysingleday.suitable.fire_immunity", "everysingleday.desc.fire_immunity", 0.0f),
            new DailyEffect("water_breathing", "everysingleday.suitable.water_breathing", "everysingleday.desc.water_breathing", 0.0f),
            new DailyEffect("vampire", "everysingleday.suitable.vampire", "everysingleday.desc.vampire", 0.3f),
            new DailyEffect("thorns", "everysingleday.suitable.thorns", "everysingleday.desc.thorns", 0.5f),
            new DailyEffect("treasure_hunter", "everysingleday.suitable.treasure_hunter", "everysingleday.desc.treasure_hunter", 0.0f),
            new DailyEffect("time_master", "everysingleday.suitable.time_master", "everysingleday.desc.time_master", 0.0f),
            new DailyEffect("super_craft", "everysingleday.suitable.super_craft", "everysingleday.desc.super_craft", 0.3f),
            new DailyEffect("animal_whisperer", "everysingleday.suitable.animal_whisperer", "everysingleday.desc.animal_whisperer", 0.0f),
            new DailyEffect("shadow_step", "everysingleday.suitable.shadow_step", "everysingleday.desc.shadow_step", 0.0f),
            new DailyEffect("elemental_shield", "everysingleday.suitable.elemental_shield", "everysingleday.desc.elemental_shield", 0.0f)
    );

    public static final List<DailyEffect> NEGATIVE_EFFECTS = Arrays.asList(
            new DailyEffect("water", "everysingleday.suitable.water", "everysingleday.desc.water", -0.5f),
            new DailyEffect("hunger", "everysingleday.suitable.hunger", "everysingleday.desc.hunger", -1.0f),
            new DailyEffect("weakness", "everysingleday.suitable.weakness", "everysingleday.desc.weakness", -0.2f),
            new DailyEffect("slowness", "everysingleday.suitable.slowness", "everysingleday.desc.slowness", -0.2f),
            new DailyEffect("mining_fatigue", "everysingleday.suitable.mining_fatigue", "everysingleday.desc.mining_fatigue", -0.3f),
            new DailyEffect("unluck", "everysingleday.suitable.unluck", "everysingleday.desc.unluck", 0.0f),
            new DailyEffect("explosive_death", "everysingleday.suitable.explosive_death", "everysingleday.desc.explosive_death", 0.0f),
            new DailyEffect("item_magnet", "everysingleday.suitable.item_magnet", "everysingleday.desc.item_magnet", 0.0f),
            new DailyEffect("sun_allergy", "everysingleday.suitable.sun_allergy", "everysingleday.desc.sun_allergy", 0.0f),
            new DailyEffect("noise_maker", "everysingleday.suitable.noise_maker", "everysingleday.desc.noise_maker", 0.0f),
            new DailyEffect("clumsy", "everysingleday.suitable.clumsy", "everysingleday.desc.clumsy", -0.2f),
            new DailyEffect("sleepwalker", "everysingleday.suitable.sleepwalker", "everysingleday.desc.sleepwalker", 0.0f),
            new DailyEffect("broken_armor", "everysingleday.suitable.broken_armor", "everysingleday.desc.broken_armor", -1.0f),
            new DailyEffect("confusion", "everysingleday.suitable.confusion", "everysingleday.desc.confusion", 0.0f),
            new DailyEffect("money_curse", "everysingleday.suitable.money_curse", "everysingleday.desc.money_curse", -1.0f),
            new DailyEffect("storm_maker", "everysingleday.suitable.storm_maker", "everysingleday.desc.storm_maker", 0.0f),
            new DailyEffect("fragile", "everysingleday.suitable.fragile", "everysingleday.desc.fragile", -0.5f),
            new DailyEffect("hated_by_animals", "everysingleday.suitable.hated_by_animals", "everysingleday.desc.hated_by_animals", 0.0f),
            new DailyEffect("gravity_well", "everysingleday.suitable.gravity_well", "everysingleday.desc.gravity_well", -0.5f)
    );

    public static final Map<UUID, PlayerDailyEffects> playerEffects = new HashMap<>();
    public static final Map<UUID, PlayerEffectState> playerEffectStates = new HashMap<>();

    public static PlayerEffectState getPlayerEffectState(ServerPlayerEntity player) {
        return playerEffectStates.computeIfAbsent(player.getUuid(), uuid -> new PlayerEffectState());
    }

    public static void applyEffectsStatic(ServerPlayerEntity target, PlayerDailyEffects data) {
        INSTANCE.applyEffects(target, data);
    }
    public static void clearOldEffectsStatic(ServerPlayerEntity target) {
        INSTANCE.clearOldEffects(target);
    }
    public static void generateNewDailyEffectsStatic(ServerPlayerEntity target, long day) {
        INSTANCE.generateNewDailyEffects(target, day);
    }

    public static EverySingleDay INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        LOGGER.info("Every Single Day mod initialized!");

        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                initializePlayerEffects(handler.getPlayer()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player)
                handlePlayerDeath(player, damageSource);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                playerEffectStates.remove(handler.getPlayer().getUuid()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EverySingleDayCommand.register(dispatcher));
    }

    /* ===================== 业务逻辑 ===================== */

    void onServerTick(MinecraftServer server) {
        if (server.getTicks() % 20 == 0) {
            checkDailyReset(server);
            handleSpecialEffects(server);
        }
        server.getPlayerManager().getPlayerList().forEach(this::handlePlayerActions);
    }

    void checkDailyReset(MinecraftServer server) {
        long now = server.getOverworld().getTimeOfDay() / 24000L;
        server.getPlayerManager().getPlayerList().forEach(p -> {
            PlayerDailyEffects data = playerEffects.computeIfAbsent(p.getUuid(), u -> new PlayerDailyEffects());
            if (data.lastDay != now) generateNewDailyEffects(p, now);
        });
    }

    void initializePlayerEffects(ServerPlayerEntity player) {
        long day = player.getServer().getOverworld().getTimeOfDay() / 24000L;
        PlayerDailyEffects data = new PlayerDailyEffects();
        data.lastDay = day;
        data.positiveEffect = randomOf(POSITIVE_EFFECTS);
        data.negativeEffect = randomOf(NEGATIVE_EFFECTS);
        playerEffects.put(player.getUuid(), data);
        applyEffects(player, data);
        sendDailyEffectsMessage(player, data, day);
    }

    void generateNewDailyEffects(ServerPlayerEntity player, long day) {
        PlayerDailyEffects data = playerEffects.get(player.getUuid());
        data.lastDay = day;
        clearOldEffects(player);
        data.positiveEffect = randomOf(POSITIVE_EFFECTS);
        data.negativeEffect = randomOf(NEGATIVE_EFFECTS);
        applyEffects(player, data);
        sendDailyEffectsMessage(player, data, day);
    }

    public void clearOldEffects(ServerPlayerEntity player) {
        player.clearStatusEffects();
        PlayerEffectState state = getPlayerEffectState(player);
        state.isShadowStepping = false;
        state.isSleepwalker = false;
        state.isConfused = false;
    }

    public void applyEffects(ServerPlayerEntity player, PlayerDailyEffects data) {
        player.clearStatusEffects();
        EffectsManager.applyPositiveEffect(player, data.positiveEffect);
        EffectsManager.applyNegativeEffect(player, data.negativeEffect);
        player.sendMessage(Text.translatable("everysingleday.daily.activated")
                .formatted(Formatting.AQUA, Formatting.BOLD), false);
    }

    /* 每日提示：完全走语言文件 */
    private void sendDailyEffectsMessage(ServerPlayerEntity player, PlayerDailyEffects effects, long day) {
        player.sendMessage(Text.translatable("everysingleday.daily.title")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.translatable("everysingleday.daily.day", day)
                .formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.translatable("everysingleday.daily.suitable",
                        effects.positiveEffect.getSuitableText(),
                        effects.positiveEffect.getDescText())
                .formatted(Formatting.GREEN), false);
        player.sendMessage(Text.translatable("everysingleday.daily.unsuitable",
                        effects.negativeEffect.getSuitableText(),
                        effects.negativeEffect.getDescText())
                .formatted(Formatting.RED), false);
        player.sendMessage(Text.translatable("everysingleday.daily.separator")
                .formatted(Formatting.GOLD), false);
    }

    /* -------------------- 各种特殊效果处理 -------------------- */
    void handlePlayerActions(ServerPlayerEntity player) {
        PlayerDailyEffects effects = playerEffects.get(player.getUuid());
        if (effects == null) return;
        PlayerEffectState state = getPlayerEffectState(player);

        if ("shadow_step".equals(effects.positiveEffect.id)) {
            if (player.isSneaking() && !state.isShadowStepping) {
                state.isShadowStepping = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 1, false, false, false));
                player.sendMessage(Text.translatable("everysingleday.shadow_step.on").formatted(Formatting.DARK_GRAY), true);
            } else if (!player.isSneaking() && state.isShadowStepping) {
                state.isShadowStepping = false;
                player.removeStatusEffect(StatusEffects.INVISIBILITY);
                player.removeStatusEffect(StatusEffects.SPEED);
                player.sendMessage(Text.translatable("everysingleday.shadow_step.off").formatted(Formatting.GRAY), true);
            }
        }
        if ("sleepwalker".equals(effects.negativeEffect.id)) handleSleepwalkerEffect(player, state);
        if ("confusion".equals(effects.negativeEffect.id)) handleConfusionEffect(player, state);
    }

    void handleSleepwalkerEffect(ServerPlayerEntity player, PlayerEffectState state) {
        if (!player.isSleeping()) { state.lastSleepPos = null; return; }
        BlockPos now = player.getBlockPos();
        if (state.lastSleepPos == null || !state.lastSleepPos.equals(now)) {
            state.lastSleepPos = now;
            state.nextSleepwalkCheck = player.getServer().getTicks() + 100 + new Random().nextInt(400);
        }
        int tick = player.getServer().getTicks();
        if (tick >= state.nextSleepwalkCheck) {
            state.nextSleepwalkCheck = tick + 40 + new Random().nextInt(160);
            if (new Random().nextInt(100) < 10) triggerSleepwalk(player);
        }
    }

    void triggerSleepwalk(ServerPlayerEntity player) {
        BlockPos cur = player.getBlockPos();
        int range = 48;
        BlockPos dest = cur.add(new Random().nextInt(range * 2) - range, 0, new Random().nextInt(range * 2) - range);
        ServerWorld world = player.getServerWorld();
        dest = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, dest);
        if (dest.getSquaredDistance(cur) < 2000
                && world.getBlockState(dest).isAir()
                && world.getBlockState(dest.down()).isSolid()) {
            player.wakeUp();
            player.requestTeleport(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
            world.playSound(null, dest, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1, 1);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
            player.sendMessage(Text.translatable("everysingleday.sleepwalker.teleport").formatted(Formatting.DARK_PURPLE));
        }
    }

    void handleConfusionEffect(ServerPlayerEntity player, PlayerEffectState state) {
        int now = player.getServer().getTicks();
        if (state.nextConfusionCheck == 0) state.nextConfusionCheck = now + 60 + new Random().nextInt(240);
        if (now >= state.nextConfusionCheck) {
            state.nextConfusionCheck = now + 40 + new Random().nextInt(200);
            applyConfusionRotation(player);
        }
    }

    void applyConfusionRotation(ServerPlayerEntity player) {
        float yaw = player.getYaw() + (new Random().nextFloat() - 0.5f) * 180;
        float pitch = Math.max(-90, Math.min(90, (new Random().nextFloat() - 0.5f) * 120));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.sendMessage(Text.translatable("everysingleday.confusion.spin").formatted(Formatting.LIGHT_PURPLE), true);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, false, false));
        if (new Random().nextInt(100) < 25) {
            Vec3d off = new Vec3d((new Random().nextDouble() - 0.5) * 8, 0, (new Random().nextDouble() - 0.5) * 8);
            Vec3d tgt = player.getPos().add(off);
            BlockPos bp = new BlockPos((int) tgt.x, (int) player.getY(), (int) tgt.z);
            if (player.getWorld().getBlockState(bp).isAir() && player.getWorld().getBlockState(bp.down()).isSolid()) {
                player.requestTeleport(tgt.x, player.getY(), tgt.z);
                player.getWorld().playSound(null, bp, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 2f);
            }
        }
    }

    void handleSpecialEffects(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDailyEffects data = playerEffects.get(player.getUuid());
            if (data == null || data.lastDay != server.getOverworld().getTimeOfDay() / 24000L) continue;
            switch (data.positiveEffect.id) {
                case "magnet" -> handleMagnetEffect(player);
                case "sun_allergy" -> handleSunAllergy(player);
                case "noise_maker" -> handleNoiseMaker(player);
                case "storm_maker" -> handleStormMaker(player);
            }
        }
    }

    void handleMagnetEffect(ServerPlayerEntity player) {
        Box box = new Box(player.getPos(), player.getPos()).expand(8);
        for (ItemEntity e : player.getWorld().getEntitiesByClass(ItemEntity.class, box, i -> true)) {
            if (e.getPos().distanceTo(player.getPos()) > 2) {
                e.setVelocity(player.getPos().subtract(e.getPos()).normalize().multiply(0.1));
                e.velocityModified = true;
            }
        }
    }

    void handleSunAllergy(ServerPlayerEntity player) {
        if (player.getWorld().isDay() && player.getWorld().isSkyVisible(player.getBlockPos()) && !player.isSpectator()
                && new Random().nextInt(100) < 2) {
            player.damage(player.getDamageSources().generic(), 1);
            player.sendMessage(Text.translatable("everysingleday.sun_allergy.damage").formatted(Formatting.YELLOW), true);
        }
    }

    void handleNoiseMaker(ServerPlayerEntity player) {
        if (new Random().nextInt(200) < 1)
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_AMBIENT,
                    SoundCategory.PLAYERS, 1, 0.8f + new Random().nextFloat() * 0.4f);
    }

    void handleStormMaker(ServerPlayerEntity player) {
        if (player.getWorld().isThundering() && new Random().nextInt(1000) < 2) {
            BlockPos pos = player.getBlockPos().add(new Random().nextInt(16) - 8, 0, new Random().nextInt(16) - 8);
            if (player.getWorld() instanceof ServerWorld sw)
                sw.setWeather(0, 6000, true, true);
        }
    }

    void handlePlayerDeath(ServerPlayerEntity player, DamageSource src) {
        PlayerDailyEffects data = playerEffects.get(player.getUuid());
        if (data == null) return;
        if ("phoenix".equals(data.positiveEffect.id)) phoenixResurrection(player);
        if ("explosive_death".equals(data.negativeEffect.id)) explosiveDeath(player);
    }

    void phoenixResurrection(ServerPlayerEntity player) {
        player.getServer().execute(() -> {
            player.setHealth(player.getMaxHealth() * 0.5f);
            player.getWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(),
                    3, false, World.ExplosionSourceType.MOB);
            player.sendMessage(Text.translatable("everysingleday.phoenix.resurrect").formatted(Formatting.GOLD, Formatting.BOLD));
        });
    }

    void explosiveDeath(ServerPlayerEntity player) {
        player.getWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(),
                5, true, World.ExplosionSourceType.MOB);
    }

    private static <T> T randomOf(List<T> list) { return list.get(new Random().nextInt(list.size())); }

    /* -------------------- 内部数据类 -------------------- */
    public static class PlayerDailyEffects {
        public long lastDay;
        public DailyEffect positiveEffect;
        public DailyEffect negativeEffect;
    }

    public static class DailyEffect {
        public final String id;
        public final String suitableKey;   // 语言 key
        public final String descKey;       // 语言 key
        public final float multiplier;
        public Object description;

        public DailyEffect(String id, String suitableKey, String descKey, float multiplier) {
            this.id = id;
            this.suitableKey = suitableKey;
            this.descKey = descKey;
            this.multiplier = multiplier;
        }

        /* 实时翻译 */
        public Text getSuitableText() {
            return Text.translatable(suitableKey);
        }
        public Text getDescText() {
            return Text.translatable(descKey);
        }
    }

    public static class PlayerEffectState {
        public boolean isShadowStepping = false;
        public boolean isSleepwalker = false;
        public boolean isConfused = false;
        public int nextSleepwalkCheck = 0;
        public int nextConfusionCheck = 0;
        public BlockPos lastSleepPos = null;
    }
}
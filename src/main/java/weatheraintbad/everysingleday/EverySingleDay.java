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

    /* ---------- 效果列表 ---------- */
    public static final List<DailyEffect> POSITIVE_EFFECTS = Arrays.asList(
            new DailyEffect("mining", "下矿", "矿物爆率提升50%", 0.5f),
            new DailyEffect("farming", "种植", "作物生长速度提升50%", 0.5f),
            new DailyEffect("combat", "战斗", "攻击力提升20%", 0.2f),
            new DailyEffect("speed", "运动", "移动速度提升30%", 0.3f),
            new DailyEffect("health", "休养", "生命恢复", 1.0f),
            new DailyEffect("luck", "碰运气", "钓鱼运气好", 0.0f),
            new DailyEffect("phoenix", "涅槃", "死亡时重生并爆炸", 0.0f),
            new DailyEffect("magnet", "收集", "自动吸引附近物品", 8.0f),
            new DailyEffect("night_vision", "熬夜", "永久夜视效果", 0.0f),
            new DailyEffect("fire_immunity", "玩火", "免疫火焰伤害", 0.0f),
            new DailyEffect("water_breathing", "潜水", "水下呼吸+游泳速度", 0.0f),
            new DailyEffect("vampire", "吸血鬼", "攻击敌人时回复生命", 0.3f),
            new DailyEffect("thorns", "战斗", "反弹敌人伤害的50%", 0.5f),
            new DailyEffect("treasure_hunter", "寻宝", "发现隐藏宝箱和稀有矿石", 0.0f),
            new DailyEffect("time_master", "时间掌控", "时间流逝速度减半", 0.0f),
            new DailyEffect("super_craft", "合成", " 合成时有几率双倍产出", 0.3f),
            new DailyEffect("animal_whisperer", "亲近自然", "动物主动跟随并保护你", 0.0f),
            new DailyEffect("shadow_step", "偷袭", "潜行时隐身+速度提升", 0.0f),
            new DailyEffect("elemental_shield", "战斗", "周期性获得随机元素保护", 0.0f)
    );

    public static final List<DailyEffect> NEGATIVE_EFFECTS = Arrays.asList(
            new DailyEffect("water", "下水", "在水中速度减慢50%", -0.5f),
            new DailyEffect("hunger", "运动", "饥饿速度提升3倍", -1.0f),
            new DailyEffect("weakness", "战斗", "攻击力降低20%", -0.2f),
            new DailyEffect("slowness", "运动", "移动速度减慢20%", -0.2f),
            new DailyEffect("mining_fatigue", "下矿", "挖掘速度减慢30%", -0.3f),
            new DailyEffect("unluck", "碰运气", "钓鱼运气差", 0.0f),
            new DailyEffect("explosive_death", "死亡", "死亡时产生爆炸", 0.0f),
            new DailyEffect("item_magnet", "收集", "无法捡起地面物品", 0.0f),
            new DailyEffect("sun_allergy", "晒太阳", "白天在阳光下持续受伤", 0.0f),
            new DailyEffect("noise_maker", "外出", "持续发出声音吸引怪物", 0.0f),
            new DailyEffect("clumsy", "手持物品", "有概率掉落手持物品", -0.2f),
            new DailyEffect("sleepwalker", "睡觉", "睡觉时随机传送到附近", 0.0f),
            new DailyEffect("broken_armor", "战斗", "装备耐久消耗速度提升2倍", -1.0f),
            new DailyEffect("confusion", "外出", "方向感错乱（视角旋转）", 0.0f),
            new DailyEffect("money_curse", "死亡", "死亡时掉落双倍经验", -1.0f),
            new DailyEffect("storm_maker", "外出", "持续吸引闪电", 0.0f),
            new DailyEffect("fragile", "战斗", "受到的伤害提高50%", -0.5f),
            new DailyEffect("hated_by_animals", "亲近自然", "动物将会主动攻击你", 0.0f),
            new DailyEffect("gravity_well", "蹦蹦跳跳", "跳跃高度降低50%摔落伤害增加50%", -0.5f)
    );

    /* ---------- 玩家数据 ---------- */
    public static final Map<UUID, PlayerDailyEffects> playerEffects = new HashMap<>();
    public static final Map<UUID, PlayerEffectState> playerEffectStates = new HashMap<>();

    public static PlayerEffectState getPlayerEffectState(ServerPlayerEntity player) {
        return playerEffectStates.computeIfAbsent(player.getUuid(), uuid -> new PlayerEffectState());
    }

    public static void applyEffectsStatic(ServerPlayerEntity target, PlayerDailyEffects data) {
    }

    public static void clearOldEffectsStatic(ServerPlayerEntity target) {
    }

    public static void generateNewDailyEffectsStatic(ServerPlayerEntity target, long day) {
    }

    @Override
    public void onInitialize() {
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

        /* 注册指令 */
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EverySingleDayCommand.register(dispatcher));
    }

    /* ------------------------------------------------------------------ */
    /* ---------------------  以下为业务逻辑，包级可见  --------------------- */
    /* ------------------------------------------------------------------ */

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
        player.sendMessage(Text.literal("✨ 今日效果已激活！")
                .formatted(Formatting.AQUA, Formatting.BOLD));
    }

    private void sendDailyEffectsMessage(ServerPlayerEntity player, PlayerDailyEffects effects, long day) {
        player.sendMessage(Text.literal("=== 每日运势 ===").formatted(Formatting.GOLD, Formatting.BOLD));
        player.sendMessage(Text.literal("游戏日: " + day).formatted(Formatting.YELLOW));
        player.sendMessage(Text.literal("宜: " + effects.positiveEffect.suitable +
                " (" + effects.positiveEffect.description + ")").formatted(Formatting.GREEN));
        player.sendMessage(Text.literal("不宜: " + effects.negativeEffect.suitable +
                " (" + effects.negativeEffect.description + ")").formatted(Formatting.RED));
        player.sendMessage(Text.literal("===============").formatted(Formatting.GOLD));
    }

    /* -------------------- 以下为各种特殊效果处理 -------------------- */
    void handlePlayerActions(ServerPlayerEntity player) {
        PlayerDailyEffects effects = playerEffects.get(player.getUuid());
        if (effects == null) return;
        PlayerEffectState state = getPlayerEffectState(player);

        if ("shadow_step".equals(effects.positiveEffect.id)) {
            if (player.isSneaking() && !state.isShadowStepping) {
                state.isShadowStepping = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 1, false, false, false));
                player.sendMessage(Text.literal("🌑 你已融入暗影中...").formatted(Formatting.DARK_GRAY), true);
            } else if (!player.isSneaking() && state.isShadowStepping) {
                state.isShadowStepping = false;
                player.removeStatusEffect(StatusEffects.INVISIBILITY);
                player.removeStatusEffect(StatusEffects.SPEED);
                player.sendMessage(Text.literal("👤 你从暗影中现身").formatted(Formatting.GRAY), true);
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
            player.sendMessage(Text.literal("😴 梦游了！你醒来发现在一个陌生的地方").formatted(Formatting.DARK_PURPLE));
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
        player.sendMessage(Text.literal("🌀 方向感突然错乱！").formatted(Formatting.LIGHT_PURPLE), true);
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
            player.sendMessage(Text.literal("阳光灼烧着你！").formatted(Formatting.YELLOW), true);
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
            player.sendMessage(Text.literal("凤凰重生！你从灰烬中复活了！").formatted(Formatting.GOLD, Formatting.BOLD));
        });
    }

    void explosiveDeath(ServerPlayerEntity player) {
        player.getWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(),
                5, true, World.ExplosionSourceType.MOB);
    }

    /* -------------------- 小工具 -------------------- */
    private static <T> T randomOf(List<T> list) { return list.get(new Random().nextInt(list.size())); }

    /* -------------------- 内部数据类 -------------------- */
    public static class PlayerDailyEffects {
        public long lastDay;
        public DailyEffect positiveEffect;
        public DailyEffect negativeEffect;
    }

    public static class DailyEffect {
        public final String id, suitable, description;
        public final float multiplier;
        public DailyEffect(String id, String suitable, String description, float multiplier) {
            this.id = id; this.suitable = suitable; this.description = description; this.multiplier = multiplier;
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
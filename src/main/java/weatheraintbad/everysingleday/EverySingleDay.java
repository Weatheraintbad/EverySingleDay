package weatheraintbad.everysingleday;

import net.fabricmc.api.ModInitializer;
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

    private static final Map<UUID, PlayerDailyEffects> playerEffects = new HashMap<>();
    private static final List<DailyEffect> POSITIVE_EFFECTS = Arrays.asList(
            // 基础效果
            new DailyEffect("mining", "下矿", "矿物爆率提升50%", 0.5f),
            new DailyEffect("farming", "种植", "作物生长速度提升50%", 0.5f),
            new DailyEffect("combat", "战斗", "攻击力提升20%", 0.2f),
            new DailyEffect("speed", "运动", "移动速度提升30%", 0.3f),
            new DailyEffect("health", "休养", "生命恢复", 1.0f),
            new DailyEffect("luck", "碰运气", "钓鱼运气好", 0.0f),

            // 新增效果
            new DailyEffect("phoenix", "涅槃", "死亡时重生并爆炸", 0.0f),
            new DailyEffect("magnet", "收集", "自动吸引附近物品", 8.0f),
            new DailyEffect("night_vision", "熬夜", "永久夜视效果", 0.0f),
            new DailyEffect("fire_immunity", "玩火", "免疫火焰伤害", 0.0f),
            new DailyEffect("water_breathing", "潜水", "水下呼吸+游泳速度", 0.0f),
            // double_jump 条目已删除
            new DailyEffect("vampire", "吸血鬼", "攻击敌人时回复生命", 0.3f),
            new DailyEffect("thorns", "战斗", "反弹敌人伤害的50%", 0.5f),
            new DailyEffect("treasure_hunter", "寻宝", "发现隐藏宝箱和稀有矿石", 0.0f),
            new DailyEffect("time_master", "时间掌控", "时间流逝速度减半", 0.0f),
            new DailyEffect("super_craft", "合成", " 合成时有几率双倍产出", 0.3f),
            new DailyEffect("animal_whisperer", "亲近自然", "动物主动跟随并保护你", 0.0f),
            new DailyEffect("shadow_step", "偷袭", "潜行时隐身+速度提升", 0.0f),
            new DailyEffect("elemental_shield", "战斗", "周期性获得随机元素保护", 0.0f)
    );

    private static final List<DailyEffect> NEGATIVE_EFFECTS = Arrays.asList(
            // 基础效果
            new DailyEffect("water", "下水", "在水中速度减慢50%", -0.5f),
            new DailyEffect("hunger", "运动", "饥饿速度提升3倍", -1.0f),
            new DailyEffect("weakness", "战斗", "攻击力降低20%", -0.2f),
            new DailyEffect("slowness", "运动", "移动速度减慢20%", -0.2f),
            new DailyEffect("mining_fatigue", "下矿", "挖掘速度减慢30%", -0.3f),
            new DailyEffect("unluck", "碰运气", "钓鱼运气差", 0.0f),

            // 新增负面效果
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

    // 新增：特殊效果状态管理
    private static final Map<UUID, playerEffectState> playerEffectStates = new HashMap<>();

    public static playerEffectState getPlayerEffectState(ServerPlayerEntity player) {
        return playerEffectStates.computeIfAbsent(player.getUuid(), uuid -> new playerEffectState());
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Every Single Day mod initialized!");

        // 注册服务器启动事件
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        // 注册玩家加入事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            initializePlayerEffects(player);
        });

        // 注册死亡事件
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity) {
                handlePlayerDeath((ServerPlayerEntity) entity, damageSource);
            }
        });

        // 新增：注册玩家断开连接事件
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerEffectStates.remove(handler.getPlayer().getUuid());
        });
    }

    private void onServerTick(MinecraftServer server) {
        // 每20 ticks检查一次（每秒）
        if (server.getTicks() % 20 == 0) {
            checkDailyReset(server);
            handleSpecialEffects(server);
        }

        // 新增：每 tick 检查玩家动作
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            handlePlayerActions(player);
        }
    }

    private void checkDailyReset(MinecraftServer server) {
        long currentDay = server.getOverworld().getTimeOfDay() / 24000L; // 获取游戏天数

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDailyEffects effects = playerEffects.get(player.getUuid());

            if (effects == null) {
                initializePlayerEffects(player);
                effects = playerEffects.get(player.getUuid());
            }

            // 检查是否需要重置（新的一天）
            if (effects.lastDay != currentDay) {
                generateNewDailyEffects(player, currentDay);
            }
        }
    }

    private void initializePlayerEffects(ServerPlayerEntity player) {
        long currentDay = player.getServer().getOverworld().getTimeOfDay() / 24000L;
        PlayerDailyEffects effects = new PlayerDailyEffects();
        effects.lastDay = currentDay;

        // 生成初始效果
        effects.positiveEffect = POSITIVE_EFFECTS.get(new Random().nextInt(POSITIVE_EFFECTS.size()));
        effects.negativeEffect = NEGATIVE_EFFECTS.get(new Random().nextInt(NEGATIVE_EFFECTS.size()));

        playerEffects.put(player.getUuid(), effects);

        // 发送消息给玩家
        sendDailyEffectsMessage(player, effects, currentDay);
    }

    private void generateNewDailyEffects(ServerPlayerEntity player, long currentDay) {
        PlayerDailyEffects effects = playerEffects.get(player.getUuid());
        effects.lastDay = currentDay;

        // 清除旧效果
        clearOldEffects(player);

        // 生成新效果
        effects.positiveEffect = POSITIVE_EFFECTS.get(new Random().nextInt(POSITIVE_EFFECTS.size()));
        effects.negativeEffect = NEGATIVE_EFFECTS.get(new Random().nextInt(NEGATIVE_EFFECTS.size()));

        // 应用新效果
        applyEffects(player, effects);

        // 发送消息给玩家
        sendDailyEffectsMessage(player, effects, currentDay);
    }

    private void clearOldEffects(ServerPlayerEntity player) {
        // 清除所有状态效果
        player.clearStatusEffects();

        // 重置特殊效果状态
        playerEffectState state = getPlayerEffectState(player);
        state.isShadowStepping = false;
        state.isSleepwalker = false;
        state.isConfused = false;
    }

    private void applyEffects(ServerPlayerEntity player, PlayerDailyEffects effects) {
        // 清除所有状态效果
        player.clearStatusEffects();

        // 应用正面效果
        EffectsManager.applyPositiveEffect(player, effects.positiveEffect);

        // 应用负面效果
        EffectsManager.applyNegativeEffect(player, effects.negativeEffect);

        // 发送效果激活消息
        player.sendMessage(Text.literal("✨ 今日效果已激活！")
                .formatted(Formatting.AQUA, Formatting.BOLD));
    }

    private void sendDailyEffectsMessage(ServerPlayerEntity player, PlayerDailyEffects effects, long day) {
        player.sendMessage(Text.literal("=== 每日运势 ===")
                .formatted(Formatting.GOLD, Formatting.BOLD));

        player.sendMessage(Text.literal("游戏日: " + day)
                .formatted(Formatting.YELLOW));

        player.sendMessage(Text.literal("宜: " + effects.positiveEffect.suitable + " (" + effects.positiveEffect.description + ")")
                .formatted(Formatting.GREEN));

        player.sendMessage(Text.literal("不宜: " + effects.negativeEffect.suitable + " (" + effects.negativeEffect.description + ")")
                .formatted(Formatting.RED));

        player.sendMessage(Text.literal("===============")
                .formatted(Formatting.GOLD));
    }

    // 新增：处理玩家动作
    private void handlePlayerActions(ServerPlayerEntity player) {
        PlayerDailyEffects effects = playerEffects.get(player.getUuid());
        if (effects == null) return;

        playerEffectState state = getPlayerEffectState(player);

        // 处理暗影步
        if (effects.positiveEffect.id.equals("shadow_step")) {
            if (player.isSneaking() && !state.isShadowStepping) {
                // 开始潜行，激活隐身
                state.isShadowStepping = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 1, false, false, false));
                player.sendMessage(Text.literal("🌑 你已融入暗影中...").formatted(Formatting.DARK_GRAY), true);
            } else if (!player.isSneaking() && state.isShadowStepping) {
                // 停止潜行，移除效果
                state.isShadowStepping = false;
                player.removeStatusEffect(StatusEffects.INVISIBILITY);
                player.removeStatusEffect(StatusEffects.SPEED);
                player.sendMessage(Text.literal("👤 你从暗影中现身").formatted(Formatting.GRAY), true);
            }
        }

        // 处理梦游
        if (effects.negativeEffect.id.equals("sleepwalker")) {
            handleSleepwalkerEffect(player, state);
        }

        // 处理混乱
        if (effects.negativeEffect.id.equals("confusion")) {
            handleConfusionEffect(player, state);
        }
    }

    // 梦游效果处理
    private void handleSleepwalkerEffect(ServerPlayerEntity player, playerEffectState state) {
        if (!player.isSleeping()) {
            state.lastSleepPos = null;
            return;
        }

        BlockPos currentSleepPos = player.getBlockPos();

        // 检查是否是新的一次睡眠
        if (state.lastSleepPos == null || !state.lastSleepPos.equals(currentSleepPos)) {
            state.lastSleepPos = currentSleepPos;
            state.nextSleepwalkCheck = player.getServer().getTicks() + 100 + new Random().nextInt(400); // 5-25秒后开始检查
        }

        int currentTick = player.getServer().getTicks();

        // 定期检查是否梦游
        if (currentTick >= state.nextSleepwalkCheck) {
            state.nextSleepwalkCheck = currentTick + 40 + new Random().nextInt(160); // 2-10秒后再次检查

            // 10% 概率触发梦游
            if (new Random().nextInt(100) < 10) {
                triggerSleepwalk(player);
            }
        }
    }

    private void triggerSleepwalk(ServerPlayerEntity player) {
        BlockPos currentPos = player.getBlockPos();

        // 寻找附近的随机位置（扩大范围）
        int range = 48; // 24格半径
        BlockPos newPos = currentPos.add(
                new Random().nextInt(range * 2) - range,
                0,
                new Random().nextInt(range * 2) - range
        );

        // 找到安全的地表位置
        ServerWorld world = player.getServerWorld();
        newPos = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, newPos);

        // 确保位置安全且不太远
        if (newPos.getSquaredDistance(currentPos) < 2000 && // 不超过50格
                world.getBlockState(newPos).isAir() &&
                world.getBlockState(newPos.down()).isSolid()) {

            // 立即醒来并传送
            player.wakeUp();
            player.requestTeleport(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);

            // 播放传送音效
            world.playSound(null, newPos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);

            // 添加短暂的迷茫效果
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));

            player.sendMessage(Text.literal("😴 梦游了！你醒来发现在一个陌生的地方")
                    .formatted(Formatting.DARK_PURPLE));
        }
    }

    // 混乱效果处理
    private void handleConfusionEffect(ServerPlayerEntity player, playerEffectState state) {
        int currentTick = player.getServer().getTicks();

        // 初始化混乱计时器
        if (state.nextConfusionCheck == 0) {
            state.nextConfusionCheck = currentTick + 60 + new Random().nextInt(240); // 3-15秒后开始
        }

        if (currentTick >= state.nextConfusionCheck) {
            state.nextConfusionCheck = currentTick + 40 + new Random().nextInt(200); // 2-12秒后再次触发

            // 应用视角旋转效果
            applyConfusionRotation(player);
        }
    }

    private void applyConfusionRotation(ServerPlayerEntity player) {
        // 随机旋转玩家的视角（更明显的旋转）
        float randomYaw = player.getYaw() + (new Random().nextFloat() - 0.5f) * 180.0f; // ±90度
        float randomPitch = (new Random().nextFloat() - 0.5f) * 120.0f; // ±60度

        // 确保俯仰角在合理范围内
        randomPitch = Math.max(-90.0f, Math.min(90.0f, randomPitch));

        // 设置玩家视角
        player.setYaw(randomYaw);
        player.setPitch(randomPitch);

        // 发送混乱消息
        player.sendMessage(Text.literal("🌀 方向感突然错乱！").formatted(Formatting.LIGHT_PURPLE), true);

        // 添加轻微的恶心效果
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, false, false));

        // 随机小距离传送（模拟空间错乱）
        if (new Random().nextInt(100) < 25) { // 25% 概率小传送
            Vec3d currentPos = player.getPos();
            Vec3d randomOffset = new Vec3d(
                    (new Random().nextDouble() - 0.5) * 8.0,  // ±4格
                    0,
                    (new Random().nextDouble() - 0.5) * 8.0   // ±4格
            );

            Vec3d newPos = currentPos.add(randomOffset);
            BlockPos blockPos = new BlockPos((int)newPos.x, (int)currentPos.y, (int)newPos.z);

            // 确保目标位置安全
            if (player.getWorld().getBlockState(blockPos).isAir() &&
                    player.getWorld().getBlockState(blockPos.down()).isSolid()) {

                player.requestTeleport(newPos.x, currentPos.y, newPos.z);

                // 播放传送音效
                player.getWorld().playSound(null, blockPos, SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                        SoundCategory.PLAYERS, 0.5f, 2.0f);
            }
        }
    }

    // 特殊效果处理方法
    private void handleSpecialEffects(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDailyEffects effects = playerEffects.get(player.getUuid());

            if (effects != null && effects.lastDay == server.getOverworld().getTimeOfDay() / 24000L) {
                // 处理磁铁效果
                if (effects.positiveEffect.id.equals("magnet")) {
                    handleMagnetEffect(player);
                }

                // 处理阳光过敏
                if (effects.negativeEffect.id.equals("sun_allergy")) {
                    handleSunAllergy(player);
                }

                // 处理噪音制造者
                if (effects.negativeEffect.id.equals("noise_maker")) {
                    handleNoiseMaker(player);
                }

                // 处理风暴召唤者
                if (effects.negativeEffect.id.equals("storm_maker")) {
                    handleStormMaker(player);
                }
            }
        }
    }

    private void handleMagnetEffect(ServerPlayerEntity player) {
        Box box = new Box(player.getPos(), player.getPos()).expand(8.0);
        List<ItemEntity> items = player.getWorld().getEntitiesByClass(ItemEntity.class, box, item -> true);

        for (ItemEntity item : items) {
            if (item.getPos().distanceTo(player.getPos()) > 2.0) {
                Vec3d direction = player.getPos().subtract(item.getPos()).normalize();
                item.setVelocity(direction.multiply(0.1));
                item.velocityModified = true;
            }
        }
    }

    private void handleSunAllergy(ServerPlayerEntity player) {
        if (player.getWorld().isDay() && player.getWorld().isSkyVisible(player.getBlockPos()) && !player.isSpectator()) {
            if (new Random().nextInt(100) < 2) {
                player.damage(player.getDamageSources().generic(), 1.0f);
                player.sendMessage(Text.literal("阳光灼烧着你！").formatted(Formatting.YELLOW), true);
            }
        }
    }

    private void handleNoiseMaker(ServerPlayerEntity player) {
        if (new Random().nextInt(200) < 1) {
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_AMBIENT,
                    SoundCategory.PLAYERS, 1.0f, 0.8f + new Random().nextFloat() * 0.4f);
        }
    }

    private void handleStormMaker(ServerPlayerEntity player) {
        if (player.getWorld().isThundering() && new Random().nextInt(1000) < 2) {
            BlockPos pos = player.getBlockPos().add(new Random().nextInt(16) - 8, 0, new Random().nextInt(16) - 8);
            if (player.getWorld() instanceof ServerWorld) {
                ((ServerWorld) player.getWorld()).setWeather(0, 6000, true, true);
            }
        }
    }

    private void handlePlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
        PlayerDailyEffects effects = playerEffects.get(player.getUuid());
        if (effects == null) return;

        // 处理凤凰效果
        if (effects.positiveEffect.id.equals("phoenix")) {
            phoenixResurrection(player);
        }

        // 处理爆炸死亡效果
        if (effects.negativeEffect.id.equals("explosive_death")) {
            explosiveDeath(player);
        }
    }

    private void phoenixResurrection(ServerPlayerEntity player) {
        player.getServer().execute(() -> {
            player.setHealth(player.getMaxHealth() * 0.5f);
            player.getWorld().createExplosion(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    3.0f,
                    false,
                    World.ExplosionSourceType.MOB
            );
            player.sendMessage(Text.literal("凤凰重生！你从灰烬中复活了！")
                    .formatted(Formatting.GOLD, Formatting.BOLD));
        });
    }

    private void explosiveDeath(ServerPlayerEntity player) {
        player.getWorld().createExplosion(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                5.0f,
                true,
                World.ExplosionSourceType.MOB
        );
    }

    // 内部类
    private static class PlayerDailyEffects {
        public long lastDay;
        public DailyEffect positiveEffect;
        public DailyEffect negativeEffect;
    }

    public static class DailyEffect {
        public final String id;
        public final String suitable;
        public final String description;
        public final float multiplier;

        public DailyEffect(String id, String suitable, String description, float multiplier) {
            this.id = id;
            this.suitable = suitable;
            this.description = description;
            this.multiplier = multiplier;
        }
    }

    // 新增：玩家效果状态类
    public static class playerEffectState {
        public boolean isShadowStepping = false;
        public boolean isSleepwalker = false;
        public boolean isConfused = false;
        public int nextSleepwalkCheck = 0;
        public int nextConfusionCheck = 0;
        public BlockPos lastSleepPos = null;
    }
}
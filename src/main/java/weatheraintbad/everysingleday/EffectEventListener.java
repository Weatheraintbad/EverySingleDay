package weatheraintbad.everysingleday;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.Heightmap;

import java.util.*;

public class EffectEventListener {

    private static final Random RAND = new Random();

    /* ===== 每日 0 点刷新 ===== */
    public static void checkDailyReset(ServerWorld world) {
        long now = world.getTimeOfDay() / 24000L;
        for (ServerPlayerEntity sp : world.getPlayers()) {
            EverySingleDay.PlayerDailyEffects data = EverySingleDay.playerEffects.get(sp.getUuid());
            if (data == null) {
                EverySingleDay.initializePlayerEffects(sp);
                continue;
            }
            if (data.lastDay != now) {
                EverySingleDay.clearOldEffectsStatic(sp);
                EverySingleDay.generateNewDailyEffectsStatic(sp, now);
            }
        }
    }

    /* ===== 注册全部事件 ===== */
    public static void register() {
        /* 1. 每 tick 大轮询（magnet / sun_allergy / noise_maker / storm_maker / elemental_shield / water / shadow_step）*/
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!(world instanceof ServerWorld)) return;
            ServerWorld sw = (ServerWorld) world;
            int tick = sw.getServer().getTicks();

            for (ServerPlayerEntity sp : sw.getPlayers()) {
                /* 1. magnet → 8 格吸物品 */
                if (hasEffect(sp, "magnet"))
                    sw.getEntitiesByClass(ItemEntity.class, new Box(sp.getBlockPos()).expand(8), e -> true)
                            .forEach(item -> {
                                if (item.getPos().distanceTo(sp.getPos()) > 1.2) {
                                    item.setVelocity(sp.getPos().subtract(item.getPos()).normalize().multiply(0.1));
                                    item.velocityModified = true;
                                }
                            });

                /* 2. sun_allergy → 白天无遮挡 2% 扣 1 血 */
                if (hasEffect(sp, "sun_allergy") && sp.getWorld().isDay() && sp.getWorld().isSkyVisible(sp.getBlockPos())
                        && !sp.isSpectator() && RAND.nextInt(100) < 2) {
                    sp.damage(sp.getDamageSources().generic(), 1);
                    sp.sendMessage(Text.translatable("everysingleday.sun_allergy.damage").formatted(Formatting.YELLOW), true);
                }

                /* 3. noise_maker → 每 200 tick 播放村民声音 */
                if (hasEffect(sp, "noise_maker") && RAND.nextInt(200) < 1)
                    sp.getWorld().playSound(null, sp.getBlockPos(), SoundEvents.ENTITY_VILLAGER_AMBIENT,
                            SoundCategory.PLAYERS, 1, 0.8f + RAND.nextFloat() * 0.4f);

                /* 4. storm_maker → 强制雷雨 + 闪电 */
                if (hasEffect(sp, "storm_maker")) {
                    if (!sw.isThundering()) sw.setWeather(0, 6000, true, true);
                    if (RAND.nextInt(1000) == 0) {
                        var lightning = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(sw);
                        lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(sp.getBlockPos().add(RAND.nextInt(16) - 8, 0, RAND.nextInt(16) - 8)));
                        sw.spawnEntity(lightning);
                    }
                }

                /* 5. elemental_shield → 每 4 秒随机元素抗性 */
                if (hasEffect(sp, "elemental_shield") && (tick % 80 == 0)) {
                    int element = RAND.nextInt(4);
                    switch (element) {
                        case 0 -> sp.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0));
                        case 1 -> sp.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 100, 0));
                        case 2 -> sp.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
                        case 3 -> sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0));
                    }
                }

                /* 6. water → 仅水下缓慢（删除海豚恩惠）*/
                if (hasEffect(sp, "water") && sp.isSubmergedInWater()) {
                    sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0, false, false, false));
                }

                /* 7. shadow_step → 潜行开关 + 状态 */
                if (hasEffect(sp, "shadow_step")) handleShadowStep(sp);
            }
        });

        /* 2. 攻击事件（combat + vampire）*/
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

            // 确保是服务器端
            if (world.isClient()) return ActionResult.PASS;

            // 保存原攻击是否成功的标志
            ActionResult originalResult = ActionResult.PASS;

            if (hasEffect(sp, "combat") && entity instanceof net.minecraft.entity.LivingEntity victim
                    && !victim.isSpectator()) {
                // 计算额外伤害
                float extraDamage = victim.getMaxHealth() * 0.2f;
                victim.damage(player.getDamageSources().playerAttack(player), extraDamage);
            }

            if (hasEffect(sp, "vampire") && entity instanceof net.minecraft.entity.LivingEntity victim
                    && !victim.isSpectator()) {
                // 吸血效果
                sp.heal(victim.getMaxHealth() * 0.15f);
            }

            // 必须返回 PASS 让原版攻击逻辑继续执行
            return ActionResult.PASS;
        });

        /* 3. 死亡事件（phoenix + explosive_death）*/
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity sp)) return;

            /* phoenix → 半血复活 + 爆炸 */
            if (hasEffect(sp, "phoenix")) {
                sp.getServer().execute(() -> {
                    sp.setHealth(sp.getMaxHealth() * 0.5f);
                    sp.getWorld().createExplosion(sp, sp.getX(), sp.getY(), sp.getZ(),
                            3, false, net.minecraft.world.World.ExplosionSourceType.MOB);
                    sp.sendMessage(net.minecraft.text.Text.translatable("everysingleday.phoenix.resurrect")
                            .formatted(Formatting.GOLD, Formatting.BOLD));
                });
            }

            /* explosive_death → 5 级爆炸（破坏方块）*/
            if (hasEffect(sp, "explosive_death")) {
                sp.getWorld().createExplosion(sp, sp.getX(), sp.getY(), sp.getZ(),
                        5, true, net.minecraft.world.World.ExplosionSourceType.MOB);
            }
        });

        /* 4. 作物催熟（farming）*/
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!(world instanceof ServerWorld)) return;
            ServerWorld sw = (ServerWorld) world;
            for (ServerPlayerEntity sp : sw.getPlayers()) {
                if (!hasEffect(sp, "farming")) continue;
                BlockPos base = sp.getBlockPos();
                BlockPos.iterate(base.add(-16, -2, -16), base.add(16, 2, 16)).forEach(pos -> {
                    var state = sw.getBlockState(pos);
                    if (state.getBlock() instanceof net.minecraft.block.CropBlock crop) {
                        if (RAND.nextInt(100) < 50) crop.randomTick(state, sw, pos, sw.random);
                    }
                });
            }
        });

        /* 5. 每日刷新*/
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world instanceof ServerWorld) {
                checkDailyReset((ServerWorld) world);
            }
        });
    }

    /* ================= 小工具 ================= */
    public static boolean hasEffect(ServerPlayerEntity player, String id) {
        EverySingleDay.PlayerDailyEffects data = EverySingleDay.playerEffects.get(player.getUuid());
        if (data == null) return false;
        return (data.positiveEffect != null && data.positiveEffect.id.equals(id)) ||
                (data.negativeEffect != null && data.negativeEffect.id.equals(id));
    }

    public static void clearPlayerEffects(ServerPlayerEntity target) {
        target.clearStatusEffects();
        // 不再清除动物 Goal（animal_whisperer 已删除）
    }

    /* ===== shadow_step 实现（内联）===== */
    private static void handleShadowStep(ServerPlayerEntity sp) {
        EverySingleDay.PlayerDailyEffects data = EverySingleDay.playerEffects.get(sp.getUuid());
        if (data == null || !data.positiveEffect.id.equals("shadow_step")) return;
        /* 潜行开关 + 状态 */
        if (sp.isSneaking()) {
            sp.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 100, 0, false, false));
            sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, false, false));
            sp.sendMessage(net.minecraft.text.Text.translatable("everysingleday.shadow_step.on"), true);
        } else {
            sp.removeStatusEffect(StatusEffects.INVISIBILITY);
            sp.removeStatusEffect(StatusEffects.SPEED);
            sp.sendMessage(net.minecraft.text.Text.translatable("everysingleday.shadow_step.off"), true);
        }
    }
}
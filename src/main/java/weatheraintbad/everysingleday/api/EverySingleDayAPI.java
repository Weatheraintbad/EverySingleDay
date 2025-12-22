package weatheraintbad.everysingleday.api;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public interface EverySingleDayAPI {
    /**
     * 获取玩家的每日效果
     */
    Optional<DailyEffects> getDailyEffects(ServerPlayerEntity player);

    /**
     * 检查玩家是否拥有特定效果
     */
    boolean hasEffect(ServerPlayerEntity player, String effectId);

    /**
     * 获取玩家今天的正面效果ID
     */
    @Nullable
    String getPositiveEffectId(ServerPlayerEntity player);

    /**
     * 获取玩家今天的负面效果ID
     */
    @Nullable
    String getNegativeEffectId(ServerPlayerEntity player);

    /**
     * 强制为玩家刷新每日效果（管理员用）
     */
    void refreshDailyEffects(ServerPlayerEntity player);

    /**
     * 效果信息类
     */
    class DailyEffects {
        private final String positiveEffectId;
        private final String negativeEffectId;
        private final long lastDay;

        public DailyEffects(String positiveEffectId, String negativeEffectId, long lastDay) {
            this.positiveEffectId = positiveEffectId;
            this.negativeEffectId = negativeEffectId;
            this.lastDay = lastDay;
        }

        public String getPositiveEffectId() { return positiveEffectId; }
        public String getNegativeEffectId() { return negativeEffectId; }
        public long getLastDay() { return lastDay; }
    }

    // 单例访问器
    static EverySingleDayAPI getInstance() {
        return EverySingleDayAPIImpl.INSTANCE;
    }
}
package weatheraintbad.everysingleday.api;

import net.minecraft.server.network.ServerPlayerEntity;
import weatheraintbad.everysingleday.EverySingleDay;
import weatheraintbad.everysingleday.EffectEventListener;

import java.util.Optional;

public class EverySingleDayAPIImpl implements EverySingleDayAPI {
    public static final EverySingleDayAPIImpl INSTANCE = new EverySingleDayAPIImpl();

    @Override
    public Optional<DailyEffects> getDailyEffects(ServerPlayerEntity player) {
        EverySingleDay.PlayerDailyEffects data = EverySingleDay.playerEffects.get(player.getUuid());
        if (data == null) return Optional.empty();

        return Optional.of(new DailyEffects(
                data.positiveEffect != null ? data.positiveEffect.id : null,
                data.negativeEffect != null ? data.negativeEffect.id : null,
                data.lastDay
        ));
    }

    @Override
    public boolean hasEffect(ServerPlayerEntity player, String effectId) {
        return EffectEventListener.hasEffect(player, effectId);
    }

    @Override
    public String getPositiveEffectId(ServerPlayerEntity player) {
        EverySingleDay.PlayerDailyEffects data = EverySingleDay.playerEffects.get(player.getUuid());
        return data != null && data.positiveEffect != null ? data.positiveEffect.id : null;
    }

    @Override
    public String getNegativeEffectId(ServerPlayerEntity player) {
        EverySingleDay.PlayerDailyEffects data = EverySingleDay.playerEffects.get(player.getUuid());
        return data != null && data.negativeEffect != null ? data.negativeEffect.id : null;
    }

    @Override
    public void refreshDailyEffects(ServerPlayerEntity player) {
        EverySingleDay.clearOldEffectsStatic(player);
        EverySingleDay.generateNewDailyEffectsStatic(player, player.getWorld().getTimeOfDay() / 24000L);
    }
}
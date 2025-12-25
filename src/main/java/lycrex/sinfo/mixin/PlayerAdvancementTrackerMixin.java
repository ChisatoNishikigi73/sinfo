package lycrex.sinfo.mixin;

import lycrex.sinfo.api.event.AdvancementHandler;
import lycrex.sinfo.api.event.EventManager;
import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/AdvancementRewards;apply(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void onAdvancementGranted(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        AdvancementHandler.handleAdvancement(owner, advancement);
    }
}


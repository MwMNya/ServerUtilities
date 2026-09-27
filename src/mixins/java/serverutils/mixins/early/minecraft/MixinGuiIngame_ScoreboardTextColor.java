package serverutils.mixins.early.minecraft;

import net.minecraft.client.gui.GuiIngame;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.client.scoreboard.AnimatedScoreboardRenderer;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame_ScoreboardTextColor {

    @Inject(method = "func_96136_a", at = @At("HEAD"), cancellable = true)
    private void serverutilities$hideVanillaSidebar(CallbackInfo ci) {
        if (AnimatedScoreboardRenderer.shouldHideVanillaSidebar()) ci.cancel();
    }

    /**
     * Vanilla 1.7.10 draws all sidebar scoreboard text with an alpha value of 0x20, making it nearly unreadable.
     */
    @ModifyConstant(method = "func_96136_a", constant = @Constant(intValue = 0x20FFFFFF), require = 3)
    private int serverutilities$makeScoreboardTextOpaque(int original) {
        return 0xFFFFFFFF;
    }
}

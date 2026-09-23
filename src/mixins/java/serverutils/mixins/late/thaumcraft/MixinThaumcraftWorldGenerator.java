package serverutils.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import serverutils.ServerUtilitiesConfig;

/**
 * Thaumcraft only generates mounds, hilltop stones and eldritch obelisks when the dimension ID is exactly zero. The
 * surface resource world uses the vanilla surface generator, so it should receive the same structures.
 */
@Pseudo
@Mixin(targets = "thaumcraft.common.lib.world.ThaumcraftWorldGenerator", remap = false)
public class MixinThaumcraftWorldGenerator {

    @ModifyExpressionValue(
            method = "generateSurface",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/WorldProvider;field_76574_g:I", ordinal = 1),
            require = 1)
    private int serverutilities$allowStructuresInSurfaceResourceWorld(int dimensionId) {
        return dimensionId == ServerUtilitiesConfig.dimension.miningDimensionId ? 0 : dimensionId;
    }
}

package serverutils.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.util.StringUtils;

@Mixin(ServerConfigurationManager.class)
public class MixinServerConfigurationManager_JoinMessage {

    @WrapOperation(
            method = "initializeConnectionToPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg(Lnet/minecraft/util/IChatComponent;)V"))
    private void serverutilities$replaceJoinMessage(ServerConfigurationManager instance, IChatComponent original,
            Operation<Void> originalCall, @Local(argsOnly = true) EntityPlayerMP player) {
        String configured = RankConfigAPI.get(player, ServerUtilitiesPermissions.LOGIN_JOIN_MESSAGE).getString();
        if (configured.isEmpty()) {
            configured = ServerUtilitiesConfig.login.join_message;
        }
        if (configured == null || configured.trim().isEmpty()) return;

        String playerName = player.getCommandSenderName();
        String displayName = player.getDisplayName();
        String oldPlayerName = "";

        if (original instanceof ChatComponentTranslation translation) {
            Object[] arguments = translation.getFormatArgs();
            if (arguments.length > 1) {
                oldPlayerName = String.valueOf(arguments[1]);
            }
        }

        String message = configured.replace("{player}", playerName).replace("{display_name}", displayName)
                .replace("{old_player}", oldPlayerName);
        originalCall.call(instance, ForgeHooks.newChatWithLinks(StringUtils.addFormatting(message)));
    }
}

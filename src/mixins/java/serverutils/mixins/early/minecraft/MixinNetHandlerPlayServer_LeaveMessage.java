package serverutils.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.util.StringUtils;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer_LeaveMessage {

    @Shadow
    public EntityPlayerMP playerEntity;

    @WrapOperation(
            method = "onDisconnect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg(Lnet/minecraft/util/IChatComponent;)V"))
    private void serverutilities$replaceLeaveMessage(ServerConfigurationManager instance, IChatComponent original,
            Operation<Void> originalCall) {
        String configured = RankConfigAPI.get(playerEntity, ServerUtilitiesPermissions.LOGIN_LEAVE_MESSAGE).getString();
        if (configured.isEmpty()) {
            configured = ServerUtilitiesConfig.login.leave_message;
        }
        if (configured == null || configured.trim().isEmpty()) return;

        String playerName = playerEntity.getCommandSenderName();
        String displayName = playerName;
        if (original instanceof ChatComponentTranslation translation) {
            Object[] arguments = translation.getFormatArgs();
            if (arguments.length > 0 && arguments[0] instanceof IChatComponent playerComponent) {
                playerName = playerComponent.getUnformattedText();
                displayName = playerComponent.getFormattedText();
            }
        }

        String message = configured.replace("{player}", playerName).replace("{display_name}", displayName);
        originalCall.call(instance, ForgeHooks.newChatWithLinks(StringUtils.addFormatting(message)));
    }
}

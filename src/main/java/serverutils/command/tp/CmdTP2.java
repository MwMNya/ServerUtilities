package serverutils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.StringUtils;

public class CmdTP2 extends CmdBase {

    public CmdTP2() {
        super("tp2", Level.OP_OR_SP);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);

        ServerUtilitiesPlayerData self = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender));

        ServerUtilitiesPlayerData other = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender, args[0]));

        if (other == null || !other.player.isOnline()) {
            IChatComponent component = ServerUtilities.lang(sender, "serverutilities.lang.tp2.cant_request");

            component.getChatStyle().setColor(EnumChatFormatting.RED);

            sender.addChatMessage(component);
            return;
        }

        EntityPlayerMP player = self.player.getPlayer();
        EntityPlayerMP target = other.player.getPlayer();

        if (player == target) {
            IChatComponent component = ServerUtilities.lang(sender, "serverutilities.lang.tp2.cant_request");

            component.getChatStyle().setColor(EnumChatFormatting.RED);

            sender.addChatMessage(component);
            return;
        }

        TeleporterDimPos targetPos = TeleporterDimPos.of(target);

        player.rotationYaw = target.rotationYaw;
        player.rotationPitch = target.rotationPitch;
        player.rotationYawHead = target.rotationYaw;

        self.teleport(targetPos, TeleportType.TP2, null);

        IChatComponent targetName = StringUtils
                .color(new ChatComponentText(target.getDisplayName()), EnumChatFormatting.BLUE);

        IChatComponent message = ServerUtilities.lang(sender, "serverutilities.lang.tp2.successfully", targetName);

        sender.addChatMessage(message);
    }
}

package serverutils.command.tp;

import static serverutils.ServerUtilitiesConfig.world;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.pregenerator.RTPPreGenManager;

public class CmdEnd extends CmdBase {

    public CmdEnd() {
        super("end", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (player.dimension != world.spawn_dimension
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.SPAWN_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.cross_dim");
        }
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, TeleportType.END);

        World world = player.mcServer.worldServerForDimension(ServerUtilitiesConfig.world.end_dimension);

        TeleporterDimPos tpDimPos = RTPPreGenManager.getRandomPreGenPosition(ServerUtilitiesConfig.world.end_dimension);
        boolean usedPreGeneratedPosition = !RTPPreGenManager.isInvalidPosition(tpDimPos);

        if (!usedPreGeneratedPosition) {
            tpDimPos = RTPPreGenManager.findBlockPosEnd(world, 0);
        }

        if (RTPPreGenManager.isInvalidPosition(tpDimPos)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.rtp.fail");
        }

        data.teleport(tpDimPos, TeleportType.END, null);

        IChatComponent component = ServerUtilities.lang(
                usedPreGeneratedPosition ? "serverutilities.lang.end.successfully"
                        : "serverutilities.lang.end.successfully1");
        component.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(component);
    }
}

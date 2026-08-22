package serverutils.command;

import static serverutils.ServerUtilitiesConfig.world;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdSetspawn extends CmdBase {

    public CmdSetspawn() {
        super("setspawn", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (player.dimension != world.spawn_dimension
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.SPAWN_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.cross_dim");
        }
        player.setSpawnChunk(player.getPlayerCoordinates(), true);
        ChunkCoordinates cc = player.getPlayerCoordinates();
        int x = cc.posX;
        int y = cc.posY;
        int z = cc.posZ;
        IChatComponent component = ServerUtilities.lang(sender, "serverutilities.lang.setspawn.successfully", x, y, z);
        component.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(component);
    }
}

package serverutils.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdSetWorldSpawn extends CmdBase {

    public CmdSetWorldSpawn() {
        super("setworldspawn", Level.OP_OR_SP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.SETWORLDSPAWN)) {
            return;
        }

        int x, y, z;

        ChunkCoordinates cc = player.getPlayerCoordinates();
        if (args.length == 3) {
            x = Integer.parseInt(args[0]);
            y = Integer.parseInt(args[1]);
            z = Integer.parseInt(args[2]);
        } else {
            x = cc.posX;
            y = cc.posY;
            z = cc.posZ;
        }
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        if (args.length == 5) {
            yaw = Float.parseFloat(args[3]);
            pitch = Float.parseFloat(args[4]);
        }
        World world = player.worldObj;
        world.setSpawnLocation(x, y, z);
        IChatComponent component = ServerUtilities
                .lang(sender, "serverutilities.lang.setworldspawn.successfully", x, y, z);
        component.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(component);
    }
}

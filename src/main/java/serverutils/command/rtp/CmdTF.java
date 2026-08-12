package serverutils.command.rtp;

import static serverutils.pregenerator.RTPPreGenManager.findBlockPos;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
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

public class CmdTF extends CmdBase {

    public CmdTF() {
        super("tf", Level.ALL);
    }

    private static final List<Block> UNSAFE_BLOCKS = Arrays
            .asList(Blocks.cactus, Blocks.fire, Blocks.lava, Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.RTP)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.rtp.no_permission");
        }
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, TeleportType.RTP);

        World world = player.mcServer.worldServerForDimension(ServerUtilitiesConfig.rtp.twilightForestDimensionID);

        TeleporterDimPos tpDimPos = RTPPreGenManager
                .getRandomPreGenPosition(ServerUtilitiesConfig.rtp.twilightForestDimensionID);

        if (tpDimPos != null) {
            data.teleport(tpDimPos, TeleportType.RTP, null);
            IChatComponent component = ServerUtilities.lang("serverutilities.lang.rtp.successfully");
            component.getChatStyle().setColor(EnumChatFormatting.GREEN);
            sender.addChatMessage(component);
            return;
        }
        TeleporterDimPos pos = findBlockPos(world, 0);
        data.teleport(pos, TeleportType.RTP, null);
        IChatComponent component = ServerUtilities.lang("serverutilities.lang.rtp.successfully1");
        component.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(component);
    }
}

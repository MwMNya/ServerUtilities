package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.command.chunks.CmdTeleport;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

/** Requests a teleport to one exact claimed chunk selected in the claims GUI. */
public class MessageClaimedChunkTeleport extends MessageToServer {

    private int dimension;
    private int chunkX;
    private int chunkZ;

    public MessageClaimedChunkTeleport() {}

    public MessageClaimedChunkTeleport(int dim, int x, int z) {
        dimension = dim;
        chunkX = x;
        chunkZ = z;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(dimension);
        data.writeVarInt(chunkX);
        data.writeVarInt(chunkZ);
    }

    @Override
    public void readData(DataIn data) {
        dimension = data.readVarInt();
        chunkX = data.readVarInt();
        chunkZ = data.readVarInt();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        ServerUtilitiesServerEventHandler.scheduleServerTask(() -> handleMessage(player));
    }

    private void handleMessage(EntityPlayerMP player) {
        if (!ClaimedChunks.isActive()) {
            player.addChatMessage(ServerUtilities.lang(player, "feature_disabled_server"));
            return;
        }

        ForgePlayer forgePlayer = ClaimedChunks.instance.universe.getPlayer(player);
        if (!forgePlayer.hasPermission(ServerUtilitiesPermissions.CLAIMS_TELEPORT)) {
            player.addChatMessage(ServerUtilities.lang(player, "commands.generic.permission"));
            return;
        }

        ChunkDimPos pos = new ChunkDimPos(chunkX, chunkZ, dimension);
        ClaimedChunk chunk = ClaimedChunks.instance.getChunk(pos);
        if (chunk == null) {
            player.addChatMessage(ServerUtilities.lang(player, "commands.chunks.tp.not_claimed", chunkX, chunkZ));
            return;
        }

        boolean ownTeam = forgePlayer.hasTeam() && chunk.getTeam().equalsTeam(forgePlayer.team);
        if (!ownTeam && !forgePlayer.hasPermission(ServerUtilitiesPermissions.CLAIMS_OTHER_TELEPORT)) {
            player.addChatMessage(ServerUtilities.lang(player, "commands.generic.permission"));
            return;
        }

        TeleporterDimPos destination = CmdTeleport.findSafePosition(pos);
        if (destination == null) {
            player.addChatMessage(
                    ServerUtilities.lang(player, "commands.chunks.tp.no_safe_chunk", chunkX, chunkZ, dimension));
            return;
        }

        player.closeScreen();
        ServerUtilitiesPlayerData.get(player).teleport(destination, TeleportType.CLAIM, null);
        player.addChatMessage(
                ServerUtilities.lang(
                        player,
                        "commands.chunks.tp.success",
                        chunk.getTeam().getTitle().getUnformattedText(),
                        chunkX,
                        chunkZ,
                        dimension));
    }
}

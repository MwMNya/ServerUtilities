package serverutils.command.chunks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdTeleport extends CmdBase {

    public CmdTeleport() {
        super("tp", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof EntityPlayerMP player
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_OTHER_TELEPORT)) {
            return getListOfStringsMatchingLastWord(args, player.mcServer.getAllUsernames());
        }
        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }
        if (args.length > 1) {
            throw ServerUtilities.error(sender, "commands.chunks.tp.usage");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_TELEPORT)) {
            throw new CommandException("commands.generic.permission");
        }
        ForgePlayer target = CommandUtils
                .getSelfOrOther(sender, args, 0, ServerUtilitiesPermissions.CLAIMS_OTHER_TELEPORT);
        if (!target.hasTeam()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        }

        List<ClaimedChunk> chunks = new ArrayList<>(
                ClaimedChunks.instance.getTeamChunks(target.team, OptionalInt.empty()));
        chunks.removeIf(ClaimedChunk::isInvalid);
        if (chunks.isEmpty()) {
            throw ServerUtilities.error(sender, "commands.chunks.tp.no_claims", target.getName());
        }

        ChunkDimPos current = new ChunkDimPos(player);
        chunks.sort(
                Comparator.comparingInt((ClaimedChunk chunk) -> chunk.getPos().dim == current.dim ? 0 : 1)
                        .thenComparingLong(chunk -> distanceSquared(current, chunk.getPos()))
                        .thenComparingInt(chunk -> chunk.getPos().dim).thenComparingInt(chunk -> chunk.getPos().posX)
                        .thenComparingInt(chunk -> chunk.getPos().posZ));

        for (ClaimedChunk chunk : chunks) {
            TeleporterDimPos destination = findSafePosition(chunk.getPos());
            if (destination == null) continue;

            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(sender);
            data.teleport(destination, TeleportType.CLAIM, null);
            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "commands.chunks.tp.success",
                            target.getName(),
                            chunk.getPos().posX,
                            chunk.getPos().posZ,
                            chunk.getPos().dim));
            return;
        }

        throw ServerUtilities.error(sender, "commands.chunks.tp.no_safe_position", target.getName());
    }

    private static long distanceSquared(ChunkDimPos from, ChunkDimPos to) {
        if (from.dim != to.dim) return Long.MAX_VALUE;
        long dx = (long) from.posX - to.posX;
        long dz = (long) from.posZ - to.posZ;
        return dx * dx + dz * dz;
    }

    public static TeleporterDimPos findSafePosition(ChunkDimPos chunk) {
        WorldServer world = DimensionManager.getWorld(chunk.dim);
        if (world == null) {
            try {
                DimensionManager.initDimension(chunk.dim);
                world = DimensionManager.getWorld(chunk.dim);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        if (world == null) return null;

        world.theChunkProviderServer.loadChunk(chunk.posX, chunk.posZ);
        int centerX = (chunk.posX << 4) + 8;
        int centerZ = (chunk.posZ << 4) + 8;
        int minX = chunk.posX << 4;
        int minZ = chunk.posZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int maxY = Math.min(254, world.getActualHeight() - 2);

        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (x < minX || x > maxX || z < minZ || z > maxZ) continue;

                    for (int y = maxY; y >= 1; y--) {
                        if (isSafePosition(world, x, y, z)) {
                            return TeleporterDimPos.of(x + 0.5D, y + 1D, z + 0.5D, chunk.dim);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafePosition(WorldServer world, int x, int y, int z) {
        Block ground = world.getBlock(x, y, z);
        if (ground.isAir(world, x, y, z) || !ground.getMaterial().isSolid()) return false;
        AxisAlignedBB groundBounds = ground.getCollisionBoundingBoxFromPool(world, x, y, z);
        if (groundBounds == null || groundBounds.maxY > y + 1.001D) return false;
        if (ground == Blocks.bedrock || ground == Blocks.cactus
                || ground == Blocks.fire
                || ground == Blocks.lava
                || ground == Blocks.flowing_lava) {
            return false;
        }
        return world.getBlock(x, y + 1, z).isAir(world, x, y + 1, z)
                && world.getBlock(x, y + 2, z).isAir(world, x, y + 2, z);
    }
}

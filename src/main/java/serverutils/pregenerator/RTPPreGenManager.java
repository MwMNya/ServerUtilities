package serverutils.pregenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.ForgeChunkManager;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesLoadedChunkManager;
import serverutils.data.TicketKey;
import serverutils.events.ServerReloadEvent;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.TeleporterDimPos;

@EventBusSubscriber
public class RTPPreGenManager {

    private static final Map<Integer, Deque<PregeneratorTask>> tasks = new HashMap<>();
    private static final Map<Integer, List<TeleporterDimPos>> preGenPositions = new HashMap<>();

    private static final Map<Integer, TicketKey> dimKeys = new HashMap<>();
    private static final Map<Integer, ForgeChunkManager.Ticket> dimTickets = new HashMap<>();

    private static int chunkPerSecond = ServerUtilitiesConfig.rtp.rtpPreGenSpeedPerSecond;

    private static long lastGenerateTime = 0;

    private static final List<Block> UNSAFE_BLOCKS = Arrays
            .asList(Blocks.cactus, Blocks.fire, Blocks.lava, Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    public static void start(int dimension, int centerChunkX, int centerChunkZ, int radius,
            TeleporterDimPos teleporterDimPos) {
        tasks.computeIfAbsent(dimension, k -> new LinkedList<>())
                .offer(new PregeneratorTask(dimension, centerChunkX, centerChunkZ, radius, teleporterDimPos));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ServerUtilitiesConfig.rtp.enableRTPPreGen) {
            return;
        }
        for (int dim : ServerUtilitiesConfig.rtp.rtpPreGenDimensions) {
            keepDimensionLoaded(dim);
            genPreGenChunks(dim);
        }

    }

    @SubscribeEvent
    public static void onServerReloadEvent(ServerReloadEvent event) {
        chunkPerSecond = ServerUtilitiesConfig.rtp.rtpPreGenSpeedPerSecond;
    }

    public static TicketKey requestTicketKey(int dimension) {
        MinecraftServer server = Universe.get().server;
        return dimKeys.computeIfAbsent(dimension, k -> new TicketKey(k, "!**RTP**!"));
    }

    public static ForgeChunkManager.Ticket requestTicket(int dimension) {
        MinecraftServer server = Universe.get().server;
        return dimTickets.computeIfAbsent(
                dimension,
                k -> ServerUtilitiesLoadedChunkManager.INSTANCE.requestTicket(server, requestTicketKey(dimension)));
    }

    public static void keepDimensionLoaded(int dimension) {
        ForgeChunkManager.forceChunk(requestTicket(dimension), new ChunkCoordIntPair(0, 0));
    }

    public static void unloadDimension(int dimension) {
        ForgeChunkManager.unforceChunk(requestTicket(dimension), new ChunkCoordIntPair(0, 0));
    }

    public static void clearDimensionState(int dimension) {
        tasks.remove(dimension);
        preGenPositions.remove(dimension);
        dimKeys.remove(dimension);
        dimTickets.remove(dimension);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastGenerateTime < 1000) {
            return;
        }

        lastGenerateTime = now;

        int count = chunkPerSecond;

        Iterator<Map.Entry<Integer, Deque<PregeneratorTask>>> it = tasks.entrySet().iterator();

        while (it.hasNext() && count > 0) {

            Deque<PregeneratorTask> queue = it.next().getValue();

            while (!queue.isEmpty() && count > 0) {

                PregeneratorTask task = queue.peek();

                if (!generate(task)) {
                    break;
                }
                count--;

                if (task.finished()) {

                    queue.poll();

                    preGenPositions.computeIfAbsent(task.dimension, k -> new ArrayList<>()).add(task.getPos());
                }
            }

            if (queue.isEmpty()) {
                it.remove();
            }
        }
    }

    private static boolean generate(PregeneratorTask task) {
        World world = Universe.get().server.worldServerForDimension(task.dimension);

        if (world == null) {
            return false;
        }

        int x = task.nextX();
        int z = task.nextZ();

        world.getChunkProvider().provideChunk(x, z);

        task.advance();

        return true;
    }

    public static TeleporterDimPos getRandomPreGenPosition(int dimension) {

        List<TeleporterDimPos> list = preGenPositions.get(dimension);

        if (list == null || list.isEmpty()) {
            genPreGenChunks(dimension);
            return null;
        }

        int index = Universe.get().server.getEntityWorld().rand.nextInt(list.size());

        TeleporterDimPos pos = list.remove(index);

        genPreGenChunks(dimension);

        return pos;
    }

    public static void genPreGenChunks(int dimension) {

        if (!ServerUtilitiesConfig.rtp.enableRTPPreGen) {
            return;
        }

        Deque<PregeneratorTask> queue = tasks.computeIfAbsent(dimension, k -> new LinkedList<>());

        List<TeleporterDimPos> ready = preGenPositions.computeIfAbsent(dimension, k -> new ArrayList<>());

        int max = ServerUtilitiesConfig.rtp.maxRTPPreGenChunkSetsNumber;
        int current = ready.size() + queue.size();
        int need = max - current;

        if (need <= 0) {
            return;
        }

        World world = Universe.get().server.worldServerForDimension(dimension);
        if (world == null) {
            return;
        }

        for (int i = 0; i < need; i++) {

            TeleporterDimPos pos;
            if (dimension == ServerUtilitiesConfig.world.end_dimension) {
                pos = findBlockPosEnd(world, 0);
            } else if (dimension == ServerUtilitiesConfig.dimension.miningDimensionIdUnderground) {
                pos = findBlockPosUnderground(world, 0);
            } else if (dimension == ServerUtilitiesConfig.world.nether_dimension) {
                pos = findNetherBlockPos(world, 0);
            } else {

                pos = findBlockPos(world, 0);
            }

            if (isInvalidPosition(pos)) {

                continue;
            }

            start(
                    dimension,
                    MathHelper.floor_double(pos.posX) >> 4,
                    MathHelper.floor_double(pos.posZ) >> 4,
                    ServerUtilitiesConfig.rtp.rtpPreGenRadius,
                    pos);
        }
    }

    public static TeleporterDimPos findBlockPos(World world, int depth) {
        while (++depth <= getMaxAttempts()) {
            double dist = getRandomRTPDistance(world);
            double angle = world.rand.nextDouble() * Math.PI * 2D;
            int x = MathHelper.floor_double(Math.cos(angle) * dist);
            int z = MathHelper.floor_double(Math.sin(angle) * dist);

            if (!isInsideWorldBorder(x, z) || isClaimed(world, x, z) || isOceanBiome(world, x, z)) {
                continue;
            }

            loadCandidateChunk(world, x, z);

            for (int y = 255; y > 0; y--) {
                if (isSafeSurfacePosition(world, x, y, z)) {
                    return TeleporterDimPos.of(x + 0.5D, y + 1.0D, z + 0.5D, world.provider.dimensionId);
                }
            }
        }
        return invalidPosition(world);
    }

    public static TeleporterDimPos findBlockPosUnderground(World world, int depth) {
        while (++depth <= getMaxAttempts()) {
            double dist = getRandomRTPDistance(world);
            double angle = world.rand.nextDouble() * Math.PI * 2D;
            int x = MathHelper.floor_double(Math.cos(angle) * dist);
            int z = MathHelper.floor_double(Math.sin(angle) * dist);

            if (!isInsideWorldBorder(x, z) || isClaimed(world, x, z)) {
                continue;
            }

            loadCandidateChunk(world, x, z);

            for (int y = 255; y > 0; y--) {
                if (isSafeSurfacePosition(world, x, y, z)) {
                    return TeleporterDimPos.of(x + 0.5D, y + 1.0D, z + 0.5D, world.provider.dimensionId);
                }
            }
        }
        return invalidPosition(world);
    }

    private static boolean isInsideWorldBorder(double x, double z) {
        return x > -30000000 && x < 30000000 && z > -30000000 && z < 30000000;
    }

    private static int getMaxAttempts() {
        return Math.max(0, ServerUtilitiesConfig.world.rtp_max_tries);
    }

    private static double getRandomRTPDistance(World world) {
        double configuredMin = ServerUtilitiesConfig.world.rtp_min_distance;
        double configuredMax = ServerUtilitiesConfig.world.rtp_max_distance;
        double min = Math.max(0D, Math.min(configuredMin, configuredMax));
        double max = Math.max(min, Math.max(configuredMin, configuredMax));
        return min + world.rand.nextDouble() * (max - min);
    }

    private static boolean isClaimed(World world, int x, int z) {
        return ClaimedChunks.instance != null
                && ClaimedChunks.instance.getChunk(new ChunkDimPos(x >> 4, z >> 4, world.provider.dimensionId)) != null;
    }

    /**
     * World#getBlock uses ChunkProviderServer#provideChunk internally. On a dedicated/integrated server, provideChunk
     * is allowed to return the shared EmptyChunk when the requested chunk is not already loaded. RTP searches distant,
     * normally-unloaded chunks, so every block would otherwise look like air and all attempts would fail. loadChunk
     * explicitly and synchronously loads or generates the candidate first.
     */
    private static void loadCandidateChunk(World world, int blockX, int blockZ) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).theChunkProviderServer.loadChunk(blockX >> 4, blockZ >> 4);
            return;
        }

        boolean previousFindingSpawnPoint = world.findingSpawnPoint;
        try {
            world.findingSpawnPoint = true;
            world.getChunkFromChunkCoords(blockX >> 4, blockZ >> 4);
        } finally {
            world.findingSpawnPoint = previousFindingSpawnPoint;
        }
    }

    private static boolean isSafeSurfacePosition(World world, int x, int y, int z) {
        Block ground = world.getBlock(x, y, z);
        return !ground.isAir(world, x, y, z) && ground.getMaterial().isSolid()
                && !UNSAFE_BLOCKS.contains(ground)
                && isAir(world, x, y + 1, z)
                && isAir(world, x, y + 2, z);
    }

    private static boolean isOceanBiome(World world, int x, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome.biomeName.contains("Ocean");
    }

    public static TeleporterDimPos findNetherBlockPos(World world, int depth) {
        while (++depth <= getMaxAttempts()) {
            double dist = getRandomRTPDistance(world);
            double angle = world.rand.nextDouble() * Math.PI * 2D;
            int x = MathHelper.floor_double(Math.cos(angle) * dist);
            int z = MathHelper.floor_double(Math.sin(angle) * dist);

            if (!isInsideWorldBorder(x, z) || isClaimed(world, x, z)) {
                continue;
            }

            loadCandidateChunk(world, x, z);

            for (int y = 120; y >= 5; y--) {
                if (isSafeNetherPosition(world, x, y, z)) {
                    return TeleporterDimPos.of(x + 0.5D, y + 1.0D, z + 0.5D, world.provider.dimensionId);
                }
            }
        }
        return invalidPosition(world);
    }

    private static boolean isSafeNetherPosition(World world, int x, int y, int z) {

        Block ground = world.getBlock(x, y, z);
        Block feet = world.getBlock(x, y + 1, z);
        Block head = world.getBlock(x, y + 2, z);

        if (ground.isAir(world, x, y, z)) {
            return false;
        }

        if (!feet.isAir(world, x, y + 1, z)) {
            return false;
        }

        if (!head.isAir(world, x, y + 2, z)) {
            return false;
        }

        if (UNSAFE_BLOCKS.contains(ground)) {
            return false;
        }

        if (ground == Blocks.lava || ground == Blocks.flowing_lava) {
            return false;
        }

        Block below = world.getBlock(x, y - 1, z);

        if (below == Blocks.lava || below == Blocks.flowing_lava) {
            return false;
        }

        if (!isNetherSpaceClear(world, x, y, z)) {
            return false;
        }

        if (isNearBedrock(world, x, y, z)) {
            return false;
        }

        return true;
    }

    private static boolean isNetherSpaceClear(World world, int x, int y, int z) {

        if (!isAir(world, x + 1, y + 1, z)) {
            return false;
        }

        if (!isAir(world, x - 1, y + 1, z)) {
            return false;
        }

        if (!isAir(world, x, y + 1, z + 1)) {
            return false;
        }

        if (!isAir(world, x, y + 1, z - 1)) {
            return false;
        }

        return true;
    }

    private static boolean isAir(World world, int x, int y, int z) {

        Block block = world.getBlock(x, y, z);

        return block.isAir(world, x, y, z);
    }

    private static boolean isNearBedrock(World world, int x, int y, int z) {

        int bedrockCount = 0;

        for (int yy = Math.max(0, y - 4); yy <= y; yy++) {

            Block block = world.getBlock(x, yy, z);

            if (block == Blocks.bedrock) {
                bedrockCount++;
            }
        }

        return bedrockCount >= 2;
    }

    public static TeleporterDimPos findBlockPosEnd(World world, int depth) {
        while (++depth <= getMaxAttempts()) {
            double radius = Math.max(1D, ServerUtilitiesConfig.rtp.endMainIslandRadius);
            double dist = Math.sqrt(world.rand.nextDouble()) * radius;
            double angle = world.rand.nextDouble() * Math.PI * 2D;
            int x = MathHelper.floor_double(Math.cos(angle) * dist);
            int z = MathHelper.floor_double(Math.sin(angle) * dist);

            if (isClaimed(world, x, z)) {
                continue;
            }

            loadCandidateChunk(world, x, z);

            for (int y = 255; y >= 4; y--) {
                if (isSafeEndPosition(world, x, y, z)) {
                    return TeleporterDimPos.of(x + 0.5, y + 1, z + 0.5, world.provider.dimensionId);
                }
            }
        }
        return invalidPosition(world);
    }

    private static boolean isSafeEndPosition(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == Blocks.end_stone && world.getBlock(x, y - 1, z) == Blocks.end_stone
                && world.getBlock(x, y - 2, z) == Blocks.end_stone
                && world.getBlock(x, y - 3, z) == Blocks.end_stone
                && world.getBlock(x, y - 4, z) == Blocks.end_stone
                && isAir(world, x, y + 1, z)
                && isAir(world, x, y + 2, z);
    }

    private static TeleporterDimPos invalidPosition(World world) {
        return TeleporterDimPos.of(-1D, -1D, -1D, world.provider.dimensionId);
    }

    public static boolean isInvalidPosition(TeleporterDimPos pos) {
        return pos == null || (pos.posX == -1 && pos.posY == -1 && pos.posZ == -1);
    }
}

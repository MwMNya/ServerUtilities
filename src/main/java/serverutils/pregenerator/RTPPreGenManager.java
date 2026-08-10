package serverutils.pregenerator;

import akka.dispatch.Foreach;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.TeleporterDimPos;

import java.util.*;

@EventBusSubscriber
public class RTPPreGenManager {

    private final Map<Integer, PregeneratorTask> tasks = new HashMap<>();

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        for (int i = 0; i < ServerUtilitiesConfig.rtp.rtpPreGenDimensions.size(); i++) {
            int dim = ServerUtilitiesConfig.rtp.rtpPreGenDimensions.get(i);
            for (int j = 0; j < 10; j++){

                findBlockPos(Universe.get().server.worldServerForDimension(dim), (EntityPlayerMP) event.player, 0);
            }
        }
    }


    public void start(int dimension, int centerChunkX, int centerChunkZ, int radius){
        tasks.put(dimension, new PregeneratorTask(dimension, centerChunkX, centerChunkZ, radius)
        );
    }

    private static final int CHUNKS_PER_TICK = 4;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event){

        if(event.phase != TickEvent.Phase.END) return;

        Iterator<PregeneratorTask> iterator = tasks.values().iterator();
        while(iterator.hasNext()){
            PregeneratorTask task = iterator.next();

            for(int i=0;i<CHUNKS_PER_TICK;i++){
                if(task.finished()){
                    iterator.remove();
                    break;
                }
                generate(task);
            }
        }
    }

    private void generate(PregeneratorTask task){
        World world = Universe.get().server.worldServerForDimension(task.dimension);

        int chunkX = task.nextX();
        int chunkZ = task.nextZ();

        world.getChunkProvider().loadChunk(chunkX, chunkZ);
        task.advance();
    }

    private static final List<Block> UNSAFE_BLOCKS = Arrays
            .asList(Blocks.cactus, Blocks.fire, Blocks.lava, Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    private static RTPPreGenManager instance;

    private TeleporterDimPos findBlockPos(World world, EntityPlayerMP player, int depth) {
        if (++depth > ServerUtilitiesConfig.world.rtp_max_tries) {
            return TeleporterDimPos.of(-1,-1,1,world.provider.dimensionId);
        }

        double dist = ServerUtilitiesConfig.world.rtp_min_distance + world.rand.nextDouble()
                * (ServerUtilitiesConfig.world.rtp_max_distance - ServerUtilitiesConfig.world.rtp_min_distance);
        double angle = world.rand.nextDouble() * Math.PI * 2D;

        int x = MathHelper.floor_double(Math.cos(angle) * dist);
        int y = 256;
        int z = MathHelper.floor_double(Math.sin(angle) * dist);

        if (!isInsideWorldBorder(world, x, y, z)) {
            return findBlockPos(world, player, depth);
        }

        if (ClaimedChunks.instance != null
                && ClaimedChunks.instance.getChunk(new ChunkDimPos(x >> 4, z >> 4, world.provider.dimensionId))
                != null) {
            return findBlockPos(world, player, depth);
        }

        if (isOceanBiome(world, x, z)) {
            return findBlockPos(world, player, depth);
        }

        while (y > 0) {
            y--;

            Block blockFeet = world.getBlock(x, y, z);
            Block blockHead = world.getBlock(x, y + 2, z);
            if (!blockFeet.equals(Blocks.air)) {
                if (blockHead.equals(Blocks.air) && !UNSAFE_BLOCKS.contains(blockFeet)) {
                    return TeleporterDimPos.of(x + 0.5D, y + 2.5D, z + 0.5D, world.provider.dimensionId);
                }
            }
        }

        return findBlockPos(world, player, depth);
    }

    private boolean isInsideWorldBorder(World world, double x, double y, double z) {
        return x > -30000000 && x < 30000000 && z > -30000000 && z < 30000000 && y > -30000000 && y < 30000000;
    }

    public boolean isOceanBiome(World world, int x, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome.biomeName.contains("Ocean");
    }

    private TeleporterDimPos findBlockPosUnderground(World world, EntityPlayerMP player, int depth) {
        if (++depth > ServerUtilitiesConfig.world.rtp_max_tries) {
            return TeleporterDimPos.of(-1,-1,1,world.provider.dimensionId);
        }

        double dist = ServerUtilitiesConfig.world.rtp_min_distance + world.rand.nextDouble()
                * (ServerUtilitiesConfig.world.rtp_max_distance - ServerUtilitiesConfig.world.rtp_min_distance);
        double angle = world.rand.nextDouble() * Math.PI * 2D;

        int x = MathHelper.floor_double(Math.cos(angle) * dist);
        int y = 256;
        int z = MathHelper.floor_double(Math.sin(angle) * dist);

        if (!isInsideWorldBorder(world, x, y, z)) {
            return findBlockPos(world, player, depth);
        }

        if (ClaimedChunks.instance != null
                && ClaimedChunks.instance.getChunk(new ChunkDimPos(x >> 4, z >> 4, world.provider.dimensionId))
                != null) {
            return findBlockPos(world, player, depth);
        }

        while (y > 0) {
            y--;

            Block blockFeet = world.getBlock(x, y, z);
            Block blockHead = world.getBlock(x, y + 2, z);
            if (!blockFeet.equals(Blocks.air)) {
                if (blockHead.equals(Blocks.air) && !UNSAFE_BLOCKS.contains(blockFeet)) {
                    return TeleporterDimPos.of(x + 0.5D, y + 2.5D, z + 0.5D, world.provider.dimensionId);
                }
            }
        }

        return findBlockPos(world, player, depth);
    }
}

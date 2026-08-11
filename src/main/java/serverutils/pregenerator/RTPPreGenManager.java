package serverutils.pregenerator;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.ForgeChunkManager;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesLoadedChunkManager;
import serverutils.data.TicketKey;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.TeleporterDimPos;

import java.util.*;


@EventBusSubscriber
public class RTPPreGenManager {

    private static final Map<Integer, Deque<PregeneratorTask>> tasks = new HashMap<>();
    private static final Map<Integer,List<TeleporterDimPos>> preGenPositions = new HashMap<>();
    private static final Map<Integer, ForgeChunkManager.Ticket> dimTickets = new HashMap<>();
    private static final Map<Integer, TicketKey> dimKeys = new HashMap<>();

    private static int chunkPerSecond = ServerUtilitiesConfig.rtp.rtpPreGenSpeedPerSecond;

    private static long lastGenerateTime = 0;

    private static final List<Block> UNSAFE_BLOCKS = Arrays.asList(
            Blocks.cactus, Blocks.fire, Blocks.lava, Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    private static ForgeChunkManager.Ticket requestTicket(int dimension){
        ForgeChunkManager.Ticket ticket = dimTickets.get(dimension);
        if(ticket != null) return ticket;
        MinecraftServer server = Universe.get().server;
        TicketKey key = new TicketKey(dimension,"!!RTP_PREGEN!!");
        ticket = ServerUtilitiesLoadedChunkManager.INSTANCE.requestTicket(server,key);
        if(ticket != null){
            dimTickets.put(dimension,ticket);
        }
        return ticket;
    }

    public static void start(int dimension, int centerChunkX, int centerChunkZ, int radius){
        tasks.computeIfAbsent(dimension, k -> new LinkedList<>())
                .offer(new PregeneratorTask(dimension, centerChunkX, centerChunkZ, radius));
    }

    @SubscribeEvent
    public static void onServerStart(FMLServerStartedEvent event){
        if (ServerUtilitiesConfig.rtp.enableRTPPreGen) {
            for(int i=0;i< ServerUtilitiesConfig.rtp.rtpPreGenDimensions.size(); i++){
                int dim = ServerUtilitiesConfig.rtp.rtpPreGenDimensions.get(i);
                if(preGenPositions.containsKey(dim)) continue;
                World world = Universe.get().server.worldServerForDimension(dim);
                if(world==null) continue;
                List<TeleporterDimPos> list = preGenPositions
                        .computeIfAbsent(dim, k->new ArrayList<>());



                for(int j=0;j<ServerUtilitiesConfig.rtp.maxRTPPreGenChunkSetsNumber;j++){
                    TeleporterDimPos pos;

                    if(dim == ServerUtilitiesConfig.dimension.miningDimensionIdUnderground){
                        pos=findBlockPosUnderground(world, 0);
                    }else{
                        pos=findBlockPos(world, 0);
                    }
                    if(pos.posX!=-1&&pos.posY!=-1&&pos.posZ!=-1){
                        list.add(pos);
                        start(dim, MathHelper.floor_double(pos.posX)>>4, MathHelper.floor_double(pos.posZ)>>4,
                                ServerUtilitiesConfig.rtp.rtpPreGenRadius);
                    }
                }
            }
        }

    }
    @SubscribeEvent
    public static void serverTick(
            TickEvent.ServerTickEvent event){

        if(event.phase != TickEvent.Phase.END)
            return;

        long now=System.currentTimeMillis();

        if(now-lastGenerateTime<1000) return;

        lastGenerateTime=now;

        int count= chunkPerSecond;

        Iterator<Map.Entry<Integer,Deque<PregeneratorTask>>> it =
                tasks.entrySet().iterator();

        while(it.hasNext() && count>0){

            Deque<PregeneratorTask> queue =
                    it.next().getValue();

            while(!queue.isEmpty() && count>0){

                PregeneratorTask task=queue.peek();

                if(task.finished()){
                    queue.poll();
                    continue;
                }
                generate(task);

                count--;

            }

            if(queue.isEmpty())
                it.remove();

        }

    }
    private static void generate(PregeneratorTask task){
        World world = Universe.get().server.worldServerForDimension(task.dimension);
        if(world==null) return;

        int x=task.nextX();
        int z=task.nextZ();

        world.getChunkProvider().provideChunk(x,z);
        ForgeChunkManager.Ticket ticket=requestTicket(task.dimension);
        if(ticket!=null) {
            ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(x, z));
        }
        task.advance();
    }

    private static TeleporterDimPos findBlockPos(World world, int depth){
        if(++depth > ServerUtilitiesConfig.world.rtp_max_tries){
            return TeleporterDimPos.of(-1, -1, -1, world.provider.dimensionId);
        }
        double dist = ServerUtilitiesConfig.world.rtp_min_distance +
                world.rand.nextDouble() * (ServerUtilitiesConfig.world.rtp_max_distance -
                        ServerUtilitiesConfig.world.rtp_min_distance);

        double angle = world.rand.nextDouble() * Math.PI*2;

        int x = MathHelper.floor_double(Math.cos(angle)*dist);
        int z = MathHelper.floor_double(Math.sin(angle)*dist);
        int y=256;

        if(!isInsideWorldBorder(world,x,y,z))
            return findBlockPos(world,depth);

        if(ClaimedChunks.instance!=null &&
                ClaimedChunks.instance.getChunk(new ChunkDimPos(x>>4, z>>4, world.provider.dimensionId))!=null){
            return findBlockPos(world,depth);
        }
        if(isOceanBiome(world,x,z))
            return findBlockPos(world,depth);

        while(y>0){
            y--;
            Block feet = world.getBlock(x,y,z);
            Block head = world.getBlock(x,y+2,z);
            if(!feet.equals(Blocks.air) && head.equals(Blocks.air) && !UNSAFE_BLOCKS.contains(feet)){
                return TeleporterDimPos.of(
                        x+0.5,
                        y+2.5,
                        z+0.5,
                        world.provider.dimensionId);
            }
        }
        return findBlockPos(world,depth);
    }

    private static TeleporterDimPos findBlockPosUnderground(World world, int depth){
        if(++depth > ServerUtilitiesConfig.world.rtp_max_tries){
            return TeleporterDimPos.of(-1,-1,-1, world.provider.dimensionId);
        }
        double dist = ServerUtilitiesConfig.world.rtp_min_distance +
                world.rand.nextDouble() *
                        (ServerUtilitiesConfig.world.rtp_max_distance
                                - ServerUtilitiesConfig.world.rtp_min_distance);

        double angle = world.rand.nextDouble()*Math.PI*2;

        int x = MathHelper.floor_double(Math.cos(angle)*dist);
        int z = MathHelper.floor_double(Math.sin(angle)*dist);
        int y=256;

        if(!isInsideWorldBorder(world,x,y,z))
            return findBlockPos(world,depth);

        if(ClaimedChunks.instance!=null &&
                ClaimedChunks.instance.getChunk(new ChunkDimPos(x>>4, z>>4, world.provider.dimensionId))!=null)
            return findBlockPos(world,depth);

        while(y>0) {
            y--;
            Block feet = world.getBlock(x, y, z);
            Block head = world.getBlock(x, y + 2, z);
            if (!feet.equals(Blocks.air) && head.equals(Blocks.air) && !UNSAFE_BLOCKS.contains(feet)) {

                return TeleporterDimPos.of(
                        x + 0.5,
                        y,
                        z + 0.5,
                        world.provider.dimensionId);
            }
        }
        return findBlockPos(world,depth);
    }

    private static boolean isInsideWorldBorder(World world, double x, double y, double z){
        return x>-30000000 && x<30000000 && z>-30000000 && z<30000000;
    }

    private static boolean isOceanBiome(World world, int x, int z){
        BiomeGenBase biome = world.getBiomeGenForCoords(x,z);
        return biome.biomeName.contains("Ocean");
    }
}
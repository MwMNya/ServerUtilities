package serverutils.dimension;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;

public class ResourceWorldProvider extends WorldProvider {

    @Override
    public void registerWorldChunkManager() {
        this.worldChunkMgr = new WorldChunkManager(worldObj);
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        long seed = -1157601049474163180L;
        return new ChunkProviderGenerate(worldObj, seed, true);
    }

    @Override
    public String getDimensionName() {
        return "Resource World";
    }

    @Override
    public boolean isSurfaceWorld() {
        return super.isSurfaceWorld();
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }
}

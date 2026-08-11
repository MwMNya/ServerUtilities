package serverutils.dimension;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;

public class ResourceWorldProviderUnderground extends WorldProvider {

    @Override
    public void registerWorldChunkManager() {
        this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenBase.ocean,0.5F);
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        long seed = 987654321L;
        return new ChunkProviderGenerate(
                worldObj, seed, true
        );
    }

    @Override
    public String getDimensionName() {
        return "Resource World";
    }

    @Override
    public String getInternalName() {
        return "resource_world";
    }

    @Override
    public boolean isSurfaceWorld() {
        return super.isSurfaceWorld();
    }

    @Override
    public boolean canRespawnHere() {
        return super.canRespawnHere();
    }
}

package se.llbit.chunky.renderer.scene.biome;

import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.ChunkTexture;
import se.llbit.chunky.world.WorldTexture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WorldTexture2dBiomeStructure implements BiomeStructure.Factory {
  static final String ID = "WORLD_TEXTURE_2D";

  @Override
  public BiomeStructure create() {
    return new Impl(new WorldTexture());
  }

  public BiomeStructure.Loader createLoader() {
    return new WorldTexture2dBiomeStructure.Loader();
  }

  @Override
  public BiomeStructure load(DataInputStream in) throws IOException {
    return new Impl(WorldTexture.load(in));
  }

  @Override
  public boolean is3d() {
    return false;
  }

  @Override
  public String getName() {
    return "World texture 2d";
  }

  @Override
  public String getDescription() {
    return "A 2d biome format that uses de-duplicated bitmaps per chunk.";
  }

  @Override
  public String getId() {
    return ID;
  }

  static class Loader extends BiomeLoader2d {
    private final WorldTexture grass = new WorldTexture();
    private final WorldTexture foliage = new WorldTexture();
    private final WorldTexture water = new WorldTexture();

    @Override
    protected void setChunk(ChunkPosition cp, float[][][] chunkGrass, float[][][] chunkFoliage, float[][][] chunkWater) {
      ChunkTexture grassTexture = new ChunkTexture();
      ChunkTexture foliageTexture = new ChunkTexture();
      ChunkTexture waterTexture = new ChunkTexture();

      for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
          grassTexture.set(x, z, chunkGrass[x][z]);
          foliageTexture.set(x, z, chunkFoliage[x][z]);
          waterTexture.set(x, z, chunkWater[x][z]);
        }
      }

      grass.setChunk(cp.x, cp.z, grassTexture);
      foliage.setChunk(cp.x, cp.z, foliageTexture);
      water.setChunk(cp.x, cp.z, waterTexture);
    }

    @Override
    public BiomeStructure buildGrass() {
      return new WorldTexture2dBiomeStructure.Impl(grass);
    }

    @Override
    public BiomeStructure buildFoliage() {
      return new WorldTexture2dBiomeStructure.Impl(foliage);
    }

    @Override
    public BiomeStructure buildWater() {
      return new WorldTexture2dBiomeStructure.Impl(water);
    }
  }

  static class Impl implements BiomeStructure {
    private final WorldTexture texture;

    public Impl(WorldTexture texture) {
      this.texture = texture;
    }

    @Override
    public void store(DataOutputStream out) throws IOException {
      texture.store(out);
    }

    @Override
    public void compact() {
      texture.compact();
    }

    @Override
    public String biomeFormat() {
      return ID;
    }

    @Override
    public void set(int x, int y, int z, float[] data) {
      texture.set(x, z, data);
    }

    @Override
    public float[] get(int x, int y, int z) {
      return texture.get(x, z);
    }
  }
}

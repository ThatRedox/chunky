package se.llbit.chunky.renderer.scene.biome;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import se.llbit.chunky.world.ChunkTexture;
import se.llbit.math.Vector3i;
import se.llbit.math.structures.Position3d2ReferencePackedArrayStructure;
import se.llbit.util.annotation.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.WeakHashMap;

public class Trivial3dBiomeStructure implements BiomeStructure.Factory {
  private static final String ID = "TRIVIAL_3D";

  @Override
  public BiomeStructure create() {
    return new Impl();
  }

  @Override
  public BiomeStructure.Loader createLoader() {
    return new Trivial3dBiomeStructure.Loader();
  }

  @Override
  public BiomeStructure load(@NotNull DataInputStream in) throws IOException {
    /*
     * Stored as:
     * (int) size
     * (int) x, y, z
     * (long) Length of present bitset in longs
     * (BitSet as longs) Present values bitset
     * (int) number of values stored
     * (float[][]) The internal data of each packed x,y,z position
     */

    Impl impl = new Impl();
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      int x = in.readInt();
      int y = in.readInt();
      int z = in.readInt();

      long[] longs = new long[in.readInt()];
      for (int bitsetIdx = 0; bitsetIdx < longs.length; bitsetIdx++) {
        longs[bitsetIdx] = in.readLong();
      }

      BitSet presentValues = BitSet.valueOf(longs);

      int count = in.readInt();
      float[][] floats = new float[count][];
      for (int idx = 0; idx < count; idx++) {
        if (presentValues.get(idx)) {
          float[] farray = new float[3];
          farray[0] = in.readFloat();
          farray[1] = in.readFloat();
          farray[2] = in.readFloat();
          floats[idx] = farray;
        }
      }
      impl.setCube(x, y, z, floats);
    }
    return impl;
  }

  @Override
  public boolean is3d() {
    return true;
  }

  @Override
  public String getName() {
    return "Trivial 3d";
  }

  @Override
  public String getDescription() {
    return "A 3d biome format that uses a packed float array to store the biomes.";
  }

  @Override
  public String getId() {
    return ID;
  }

  static class Loader extends BiomeLoader3d {
    Trivial3dBiomeStructure.Impl grass = new Impl();
    Trivial3dBiomeStructure.Impl foliage = new Impl();
    Trivial3dBiomeStructure.Impl water = new Impl();

    @Override
    protected void setChunk(int cx, int cy, int cz, Vector3i origin, ChunkAccessor chunkGrass, ChunkAccessor chunkFoliage, ChunkAccessor chunkWater) {
      cx = (cx*16 - origin.x) >> 4;
      cy = (cy*16 - origin.y) >> 4;
      cz = (cz*16 - origin.z) >> 4;
      grass.setCube(cx, cy, cz, flatten(chunkGrass));
      foliage.setCube(cx, cy, cz, flatten(chunkFoliage));
      water.setCube(cx, cy, cz, flatten(chunkWater));
    }

    private float[][] flatten(ChunkAccessor accessor) {
      float[][] out = new float[16*16*16][];
      for (int z = 0; z < 16; z++) {
        for (int y = 0; y < 16; y++) {
          for (int x = 0; x < 16; x++) {
            out[z*16*16 + y*16 + x] = accessor.get(x, y, z);
          }
        }
      }
      return out;
    }

    @Override
    public BiomeStructure buildGrass() {
      return grass;
    }

    @Override
    public BiomeStructure buildFoliage() {
      return foliage;
    }

    @Override
    public BiomeStructure buildWater() {
      return water;
    }
  }

  static class Impl extends Position3d2ReferencePackedArrayStructure<float[]> implements BiomeStructure {

    public void setCube(int x, int y, int z, float[][] data) {
      this.map.put(new XYZTriple(x, y, z), data);
    }

    @Override
    public void store(DataOutputStream out) throws IOException {
      out.writeInt(this.map.size());
      for (Object2ReferenceMap.Entry<XYZTriple, float[][]> entry : this.map.object2ReferenceEntrySet()) {
        XYZTriple key = entry.getKey();
        out.writeInt(key.x);
        out.writeInt(key.y);
        out.writeInt(key.z);
        Object[] value = entry.getValue();

        BitSet presentValues = new BitSet(value.length);
        for (int i = 0, valueLength = value.length; i < valueLength; i++) {
          presentValues.set(i, value[i] != null);
        }
        long[] longs = presentValues.toLongArray();
        out.writeInt(longs.length);
        for (long l : longs) {
          out.writeLong(l);
        }

        out.writeInt(value.length);
        for (Object o : value) {
          if (o != null) {
            for (float f : (float[]) o) {
              out.writeFloat(f);
            }
          }
        }
      }
    }

    @Override
    public String biomeFormat() {
      return ID;
    }

    @Override
    public void compact() {

    }
  }
}

/*
 * Copyright (c) 2022 Chunky contributors
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.llbit.chunky.renderer.scene.biome;

import se.llbit.chunky.world.Chunk;
import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.biome.Biome;
import se.llbit.chunky.world.biome.BiomePalette;
import se.llbit.math.Vector3i;
import se.llbit.math.structures.Position2IntStructure;

import java.util.Arrays;
import java.util.Set;

public abstract class BiomeLoader3d implements BiomeStructure.Loader {
  private final float[][][][] grassArray = new float[16][16][16][3];
  private final float[][][][] foliageArray = new float[16][16][16][3];
  private final float[][][][] waterArray = new float[16][16][16][3];

  private final ChunkAccessor grassAccessor = new ChunkAccessor(grassArray);
  private final ChunkAccessor foliageAccessor = new ChunkAccessor(foliageArray);
  private final ChunkAccessor waterAccessor = new ChunkAccessor(waterArray);

  protected static class ChunkAccessor {
    float[][][][] colors;

    ChunkAccessor(float[][][][] colors) {
      this.colors = colors;
    }

    public float[] get(int x, int y, int z) {
      return Arrays.copyOf(colors[x][y][z], 3);
    }
  }

  /**
   * @param cx            Chunk x position
   * @param cy            Chunk y position
   * @param cz            Chunk z position
   * @param origin        Scene origin
   * @param chunkGrass    Grass biome colors.
   * @param chunkFoliage  Foliage biome colors.
   * @param chunkWater    Water biome colors.
   */
  protected abstract void setChunk(int cx, int cy, int cz, Vector3i origin, ChunkAccessor chunkGrass, ChunkAccessor chunkFoliage, ChunkAccessor chunkWater);

  @Override
  public abstract BiomeStructure buildGrass();

  @Override
  public abstract BiomeStructure buildFoliage();

  @Override
  public abstract BiomeStructure buildWater();

  @Override
  public void loadBlendedChunk(ChunkPosition cp, int yMin, int yMax, Vector3i origin, Set<ChunkPosition> nonEmptyChunks, BiomePalette biomePalette, Position2IntStructure biomePaletteIdxStructure) {
    for (int sectionY = yMin >> 4; sectionY < (yMax - 1 >> 4) + 1; sectionY++) {
      for (int x = 0; x < 16; ++x) {
        for (int z = 0; z < 16; ++z) {
          for (int y = 0; y < 16; y++) {
            int nsum = 0;

            float[] grassMix = {0, 0, 0};
            float[] foliageMix = {0, 0, 0};
            float[] waterMix = {0, 0, 0};
            for (int sx = x - 1; sx <= x + 1; ++sx) {
              int wx = cp.x * 16 + sx;
              for (int sz = z - 1; sz <= z + 1; ++sz) {
                int wz = cp.z * 16 + sz;
                for (int sy = y - 1; sy < y + 1; sy++) {
                  int wy = sectionY * 16 + sy;
                  ChunkPosition ccp = ChunkPosition.get(wx >> 4, wz >> 4);
                  if (nonEmptyChunks.contains(ccp)) {
                    nsum += 1;
                    Biome biome = biomePalette.get(biomePaletteIdxStructure.get(wx, wy, wz));

                    float[] grassColor = biome.grassColorLinear;
                    grassMix[0] += grassColor[0];
                    grassMix[1] += grassColor[1];
                    grassMix[2] += grassColor[2];
                    float[] foliageColor = biome.foliageColorLinear;
                    foliageMix[0] += foliageColor[0];
                    foliageMix[1] += foliageColor[1];
                    foliageMix[2] += foliageColor[2];
                    float[] waterColor = biome.waterColorLinear;
                    waterMix[0] += waterColor[0];
                    waterMix[1] += waterColor[1];
                    waterMix[2] += waterColor[2];
                  }
                }
              }
            }

            grassMix[0] /= nsum;
            grassMix[1] /= nsum;
            grassMix[2] /= nsum;
            System.arraycopy(grassMix, 0, grassArray[x][y][z], 0, 3);

            foliageMix[0] /= nsum;
            foliageMix[1] /= nsum;
            foliageMix[2] /= nsum;
            System.arraycopy(foliageMix, 0, foliageArray[x][y][z], 0, 3);

            waterMix[0] /= nsum;
            waterMix[1] /= nsum;
            waterMix[2] /= nsum;
            System.arraycopy(waterMix, 0, waterArray[x][y][z], 0, 3);
          }
        }
      }

      setChunk(cp.x, sectionY, cp.z, origin, grassAccessor, foliageAccessor, waterAccessor);
    }
  }

  @Override
  public void loadRawChunk(ChunkPosition cp, int yMin, int yMax, Vector3i origin, BiomePalette biomePalette, Position2IntStructure biomePaletteIdxStructure) {
    for (int sectionY = yMin >> 4; sectionY < (yMax - 1 >> 4) + 1; sectionY++) {
      for (int y = 0; y < 16; y++) {
        for (int x = 0; x < 16; ++x) {
          for (int z = 0; z < 16; ++z) {
            int wx = cp.x * Chunk.X_MAX + x;
            int wy = sectionY * 16 + y;
            int wz = cp.z * Chunk.Z_MAX + z;

            Biome biome = biomePalette.get(biomePaletteIdxStructure.get(wx, wy, wz));
            System.arraycopy(biome.grassColorLinear, 0, grassArray[x][y][z], 0, 3);
            System.arraycopy(biome.foliageColorLinear, 0, foliageArray[x][y][z], 0, 3);
            System.arraycopy(biome.waterColorLinear, 0, waterArray[x][y][z], 0, 3);
          }
        }
      }
      setChunk(cp.x, sectionY >> 4, cp.z, origin, grassAccessor, foliageAccessor, waterAccessor);
    }
  }
}

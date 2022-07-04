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

import se.llbit.chunky.world.ChunkPosition;

import java.util.Arrays;

public class TrivialBiomeLoader2d extends BiomeLoader2d {
  protected final BiomeStructure grass;
  protected final BiomeStructure foliage;
  protected final BiomeStructure water;

  public TrivialBiomeLoader2d(BiomeStructure grass, BiomeStructure foliage, BiomeStructure water) {
    this.grass = grass;
    this.foliage = foliage;
    this.water = water;
  }

  @Override
  protected void setChunk(ChunkPosition cp, float[][][] chunkGrass, float[][][] chunkFoliage, float[][][] chunkWater) {
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int wx = x + cp.x*16;
        int wz = z + cp.z*16;

        float[] grassColor = Arrays.copyOf(chunkGrass[x][z], 3);
        grass.set(wx, 0, wz, grassColor);

        float[] foliageColor = Arrays.copyOf(chunkFoliage[x][z], 3);
        foliage.set(wx, 0, wz, foliageColor);

        float[] waterColor = Arrays.copyOf(chunkWater[x][z], 3);
        water.set(wx, 0, wz, waterColor);
      }
    }
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

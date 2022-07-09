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
import se.llbit.math.Vector3i;

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
  protected void setChunk(ChunkPosition cp, Vector3i origin, ChunkAccessor chunkGrass, ChunkAccessor chunkFoliage, ChunkAccessor chunkWater) {
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int wx = cp.x*16 + x - origin.x;
        int wz = cp.z*16 + z - origin.z;

        grass.set(wx, 0, wz, chunkGrass.get(x, z));
        foliage.set(wx, 0, wz, chunkFoliage.get(x, z));
        water.set(wx, 0, wz, chunkFoliage.get(x, z));
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
